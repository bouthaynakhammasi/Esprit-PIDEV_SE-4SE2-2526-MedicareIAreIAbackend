package com.aziz.demosec.service;

import com.aziz.demosec.Entities.*;
import com.aziz.demosec.delivery.DeliveryGateway;
import com.aziz.demosec.delivery.DeliveryRequest;
import com.aziz.demosec.delivery.DeliveryResponse;
import com.aziz.demosec.domain.User;
import com.aziz.demosec.dto.pharmacy.*;
import com.aziz.demosec.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PharmacyOrderServiceImpl implements PharmacyOrderService {

    private final PharmacyOrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderTrackingRepository trackingRepository;
    private final PharmacyStockRepository stockRepository;
    private final PharmacyRepository pharmacyRepository;
    private final ProductRepository productRepository;
    private final DeliveryRepository deliveryRepository;
    private final NotificationRepository notificationRepository;
    private final PaymentRepository paymentRepository;
    private final DeliveryGateway deliveryGateway;
    private final PharmacistRepository pharmacistRepository;
    private final UserRepository userRepository;
    private final WebSocketNotificationService webSocketNotificationService;
    private final EntityManager entityManager;
    private final OsrmService osrmService;
    private final GeocodingService geocodingService;
    private final DeliveryPredictionService deliveryPredictionService;
    private final EmailService emailService;

    // ──────────────────────────────────────────────────────────────
    // CREATE ORDER (PENDING)
    // ──────────────────────────────────────────────────────────────
    @Override
    public PharmacyOrderResponseDTO createOrder(PharmacyOrderRequestDTO dto) {
        Pharmacy pharmacy = pharmacyRepository.findById(dto.getPharmacyId())
                .orElseThrow(() -> new EntityNotFoundException("Pharmacy not found"));

        PharmacyOrder order = new PharmacyOrder();
        order.setPatient(findUserOrThrow(dto.getPatientId()));
        order.setPharmacy(pharmacy);
        order.setStatus(PharmacyOrderStatus.PENDING);
        order.setDeliveryAddress(dto.getDeliveryAddress());
        order.setScheduledDeliveryDate(dto.getScheduledDeliveryDate());
        order.setPrescriptionImageUrl(dto.getPrescriptionImageUrl());
        order.setDeliveryType(dto.getDeliveryType());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        // Validate stock + calculate total
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemRequestDTO itemDTO : dto.getItems()) {
            PharmacyStock stock = stockRepository
                    .findByPharmacy_IdAndProduct_Id(dto.getPharmacyId(), itemDTO.getProductId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Product " + itemDTO.getProductId() + " not available in this pharmacy"));

            if (stock.getTotalQuantity() < itemDTO.getQuantity()) {
                throw new IllegalStateException(
                        "Insufficient stock for product: " + stock.getProduct().getName());
            }
            BigDecimal lineTotal = stock.getUnitPrice().multiply(BigDecimal.valueOf(itemDTO.getQuantity()));
            total = total.add(lineTotal);
        }
        order.setTotalPrice(total);

        // ML Prediction — uniquement pour HOME_DELIVERY
        if (dto.getDeliveryType() == DeliveryType.HOME_DELIVERY && dto.getDeliveryAddress() != null) {
            try {
                double[] toCoords = geocodingService.getCoordinates(dto.getDeliveryAddress());
                System.out.println("[ML] Delivery address: " + dto.getDeliveryAddress()
                        + " → coords: " + toCoords[0] + ", " + toCoords[1]);

                double distanceKm;

                // Utiliser les coordonnées GPS de la pharmacie si disponibles
                if (pharmacy.getLocationLat() != null && pharmacy.getLocationLng() != null) {
                    System.out.println("[ML] Pharmacy GPS: " + pharmacy.getLocationLat() + ", " + pharmacy.getLocationLng());
                    distanceKm = osrmService.getDistanceKmByCoords(
                        pharmacy.getLocationLat(), pharmacy.getLocationLng(),
                        toCoords[0], toCoords[1]
                    );
                } else {
                    System.out.println("[ML] No pharmacy GPS — using address geocoding");
                    distanceKm = osrmService.getDistanceKm(pharmacy.getAddress(), dto.getDeliveryAddress());
                }

                System.out.println("[ML] Computed distance: " + distanceKm + " km");

                String city = geocodingService.extractCity(dto.getDeliveryAddress());
                System.out.println("[ML] Extracted city: " + city + ", items: " + dto.getItems().size());
                int prediction = deliveryPredictionService.predict(distanceKm, city, dto.getItems().size());
                System.out.println("[ML] Predicted delivery: " + prediction + " min");
                order.setEstimatedDeliveryMin(prediction);
            } catch (Exception e) {
                System.err.println("ML prediction skipped: " + e.getMessage());
            }
        }

        PharmacyOrder saved = orderRepository.save(order);

        // Save order items
        for (OrderItemRequestDTO itemDTO : dto.getItems()) {
            PharmacyStock stock = stockRepository
                    .findByPharmacy_IdAndProduct_Id(dto.getPharmacyId(), itemDTO.getProductId())
                    .orElseThrow();
            OrderItem item = new OrderItem();
            item.setOrder(saved);
            item.setProduct(stock.getProduct());
            item.setQuantity(itemDTO.getQuantity());
            item.setPrice(stock.getUnitPrice().multiply(BigDecimal.valueOf(itemDTO.getQuantity())));
            orderItemRepository.save(item);
        }

        // Log tracking
        addTracking(saved, PharmacyOrderStatus.PENDING, "Order placed by patient", "PATIENT");

        // Notify pharmacist
        notifyPharmacy(pharmacy, saved, NotificationType.ORDER_CREATED,
                "Nouvelle commande #" + saved.getId(),
                "Un patient a passé une nouvelle commande nécessitant validation.");

        // Email confirmation to patient
        try {
            User patient = saved.getPatient();
            if (patient != null && patient.getEmail() != null) {
                emailService.sendOrderConfirmation(
                        patient.getEmail(),
                        patient.getFullName(),
                        saved.getId(),
                        pharmacy.getName(),
                        saved.getTotalPrice(),
                        saved.getEstimatedDeliveryMin()
                );
            }
        } catch (Exception e) {
            System.err.println("Email order confirmation failed: " + e.getMessage());
        }

        return toDTO(orderRepository.findById(saved.getId()).orElseThrow());
    }

    // ──────────────────────────────────────────────────────────────
    // UPDATE STATUS (Workflow Engine)
    // ──────────────────────────────────────────────────────────────
    @Override
    public PharmacyOrderResponseDTO updateStatus(Long orderId, UpdateOrderStatusRequestDTO dto) {
        PharmacyOrder order = findOrderOrThrow(orderId);
        PharmacyOrderStatus newStatus = dto.getStatus();

        // Utilisation de la méthode JPQL (Optimisation)
        orderRepository.updateOrderStatus(orderId, newStatus, LocalDateTime.now());

        // Re-récupérer l'objet pour la suite du traitement si nécessaire
        // (Notification...)
        order = findOrderOrThrow(orderId);

        if (dto.getNote() != null)
            order.setPharmacistNote(dto.getNote());
        if (dto.getDeliveryType() != null)
            order.setDeliveryType(dto.getDeliveryType());

        String actor = dto.getChangedBy() != null ? dto.getChangedBy() : "SYSTEM";
        addTracking(order, newStatus, dto.getNote(), actor);

        // Workflow actions per status
        switch (newStatus) {
            case VALIDATED -> {
                // Décrémenter le stock à la validation
                for (OrderItem item : order.getItems()) {
                    PharmacyStock stock = stockRepository
                            .findByPharmacy_IdAndProduct_Id(order.getPharmacy().getId(), item.getProduct().getId())
                            .orElse(null);
                    if (stock != null) {
                        int newQty = stock.getTotalQuantity() - item.getQuantity();
                        if (newQty < 0)
                            throw new IllegalStateException(
                                    "Stock insuffisant pour " + item.getProduct().getName());
                        stock.setTotalQuantity(newQty);
                        stockRepository.save(stock);
                    }
                }
                // Notify patient
                notifyUser(order.getPatient(), order, NotificationType.DELIVERY_CHOICE_REQUIRED,
                        "Commande validée ✅",
                        "Votre commande #" + orderId + " est validée. Choisissez votre mode de réception.");
                // Email
                sendStatusEmail(order, "VALIDATED", dto.getNote());
            }
            case PAID -> {
                // Payment confirmed → notify pharmacist
                notifyPharmacy(order.getPharmacy(), order, NotificationType.PAYMENT_CONFIRMED,
                        "Paiement reçu 💰",
                        "Le paiement pour la commande #" + orderId + " a été confirmé.");
            }
            case DELIVERY_REQUESTED -> {
                // ✅ PAYMENT VERIFICATION: Cannot dispatch if not paid
                verifyPaymentCompleted(order);

                if (dto.getDeliveryType() == DeliveryType.HOME_DELIVERY) {
                    // Auto-assign delivery via external agency
                    triggerDelivery(order);
                }
                // Notify patient
                notifyUser(order.getPatient(), order, NotificationType.DELIVERY_ASSIGNED,
                        "Livraison demandée 🚚",
                        "La livraison pour votre commande #" + orderId
                                + " a été demandée. Un livreur sera assigné bientôt.");
            }
            case READY_FOR_PICKUP -> {
                if (dto.getDeliveryType() == DeliveryType.HOME_DELIVERY) {
                    // Auto-assign delivery via external agency
                    triggerDelivery(order);
                }
            }
            case ASSIGNED -> {
                notifyUser(order.getPatient(), order, NotificationType.DELIVERY_ASSIGNED,
                        "Livreur assigné 🚚",
                        "Un livreur a été assigné à votre commande #" + orderId + ".");
            }
            case OUT_FOR_DELIVERY -> {
                notifyUser(order.getPatient(), order, NotificationType.OUT_FOR_DELIVERY,
                        "En route 🚚",
                        "Votre commande #" + orderId + " est en route vers vous !");
                sendStatusEmail(order, "OUT_FOR_DELIVERY", null);
            }
            case DELIVERED -> {
                notifyUser(order.getPatient(), order, NotificationType.DELIVERED,
                        "Livraison effectuée ✅",
                        "Votre commande #" + orderId + " a été livrée avec succès.");
                sendStatusEmail(order, "DELIVERED", null);
            }
            default -> {
                /* no extra action */ }
        }

        return toDTO(orderRepository.save(order));
    }

    // ──────────────────────────────────────────────────────────────
    // GET METHODS
    // ──────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public PharmacyOrderResponseDTO getById(Long id) {
        return toDTO(findOrderOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PharmacyOrderResponseDTO> getAll() {
        return orderRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PharmacyOrderResponseDTO> getByPatient(Long patientId) {
        return orderRepository.findByPatient_Id(patientId).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PharmacyOrderResponseDTO> getByPharmacy(Long pharmacyId) {
        return orderRepository.findByPharmacy_Id(pharmacyId).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PharmacyOrderResponseDTO> getByPharmacyAndStatus(Long pharmacyId, String status) {
        PharmacyOrderStatus parsedStatus = PharmacyOrderStatus.valueOf(status.toUpperCase());
        return orderRepository.findByPharmacy_Id(pharmacyId).stream()
                .filter(o -> o.getStatus() == parsedStatus)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public PharmacyOrderResponseDTO cancelOrder(Long orderId, String reason) {
        PharmacyOrder order = findOrderOrThrow(orderId);
        if (order.getStatus() != PharmacyOrderStatus.PENDING) {
            throw new IllegalStateException("Only PENDING orders can be cancelled. Current: " + order.getStatus());
        }
        order.setStatus(PharmacyOrderStatus.CANCELLED);
        order.setPharmacistNote("Annulé par le patient : " + reason);
        order.setUpdatedAt(LocalDateTime.now());
        addTracking(order, PharmacyOrderStatus.CANCELLED, reason, "PATIENT");
        notifyPharmacy(order.getPharmacy(), order, NotificationType.ORDER_CREATED,
                "Commande annulée ❌", "La commande #" + orderId + " a été annulée par le patient.");
        return toDTO(orderRepository.save(order));
    }

    @Override
    @Transactional
    public PharmacyOrderResponseDTO rejectOrder(Long orderId, RejectOrderRequestDTO dto) {
        System.out.println(">>> REJECTING ORDER #" + orderId + " - Reason: " + dto.getNote());
        try {
            PharmacyOrder order = findOrderOrThrow(orderId);

            // Idempotency: already rejected
            if (order.getStatus() == PharmacyOrderStatus.REJECTED) {
                return toDTO(order);
            }

            // Can reject orders that haven't been shipped yet
            if (order.getStatus() != PharmacyOrderStatus.PENDING
                    && order.getStatus() != PharmacyOrderStatus.REVIEWING
                    && order.getStatus() != PharmacyOrderStatus.VALIDATED) {
                throw new IllegalStateException("Cannot reject an order in status: " + order.getStatus());
            }

            // Truncate note if too long
            String note = dto.getNote();
            if (note != null && note.length() > 950)
                note = note.substring(0, 947) + "...";

            // Note: clearAutomatically = true in repository handles cache eviction
            orderRepository.updateOrderStatusWithNote(orderId, PharmacyOrderStatus.REJECTED, note, LocalDateTime.now());

            // Re-fetch fresh state
            PharmacyOrder saved = findOrderOrThrow(orderId);

            // Log tracking entry
            addTracking(saved, PharmacyOrderStatus.REJECTED, note, dto.getChangedBy() != null ? dto.getChangedBy() : "PHARMACIST");

            // Notify patient
            String message = "Votre commande #" + orderId + " a été refusée : " + note;
            if (message.length() > 950)
                message = message.substring(0, 947) + "...";

            notifyUser(saved.getPatient(), saved, NotificationType.ORDER_REJECTED,
                    "Commande refusée ❌", message);

            // Email rejection to patient
            sendStatusEmail(saved, "REJECTED", note);

            System.out.println(">>> ORDER #" + orderId + " REJECTED SUCCESSFULLY");
            return toDTO(saved);
        } catch (Exception e) {
            System.err.println("❌ ERROR IN rejectOrder: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PharmacyStatsResponseDTO getPharmacyStats(Long pharmacyId) {
        List<PharmacyOrder> orders = orderRepository.findByPharmacy_Id(pharmacyId);
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new EntityNotFoundException("Pharmacy not found: " + pharmacyId));

        BigDecimal totalRevenue = orders.stream()
                .filter(o -> o.getStatus() == PharmacyOrderStatus.DELIVERED
                        || o.getStatus() == PharmacyOrderStatus.PAID)
                .map(PharmacyOrder::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Top products by quantity sold
        Map<Long, Long> productQtyMap = orders.stream()
                .filter(o -> o.getStatus() == PharmacyOrderStatus.DELIVERED
                        || o.getStatus() == PharmacyOrderStatus.PAID)
                .flatMap(o -> o.getItems().stream())
                .collect(Collectors.groupingBy(i -> i.getProduct().getId(),
                        Collectors.summingLong(OrderItem::getQuantity)));

        Map<Long, BigDecimal> productRevenueMap = orders.stream()
                .filter(o -> o.getStatus() == PharmacyOrderStatus.DELIVERED
                        || o.getStatus() == PharmacyOrderStatus.PAID)
                .flatMap(o -> o.getItems().stream())
                .collect(Collectors.groupingBy(i -> i.getProduct().getId(),
                        Collectors.reducing(BigDecimal.ZERO, OrderItem::getPrice, BigDecimal::add)));

        List<PharmacyStatsResponseDTO.TopProductDTO> topProducts = productQtyMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    OrderItem sample = orders.stream()
                            .flatMap(o -> o.getItems().stream())
                            .filter(i -> i.getProduct().getId().equals(e.getKey()))
                            .findFirst().orElse(null);
                    return PharmacyStatsResponseDTO.TopProductDTO.builder()
                            .productId(e.getKey())
                            .productName(sample != null ? sample.getProduct().getName() : "Unknown")
                            .totalQuantitySold(e.getValue())
                            .totalRevenue(productRevenueMap.getOrDefault(e.getKey(), BigDecimal.ZERO))
                            .build();
                })
                .collect(Collectors.toList());

        return PharmacyStatsResponseDTO.builder()
                .pharmacyId(pharmacyId)
                .pharmacyName(pharmacy.getName())
                .totalOrders(orders.size())
                .pendingOrders(orders.stream().filter(o -> o.getStatus() == PharmacyOrderStatus.PENDING).count())
                .validatedOrders(orders.stream().filter(o -> o.getStatus() == PharmacyOrderStatus.VALIDATED).count())
                .rejectedOrders(orders.stream().filter(o -> o.getStatus() == PharmacyOrderStatus.REJECTED).count())
                .cancelledOrders(orders.stream().filter(o -> o.getStatus() == PharmacyOrderStatus.CANCELLED).count())
                .deliveredOrders(orders.stream().filter(o -> o.getStatus() == PharmacyOrderStatus.DELIVERED).count())
                .totalRevenue(totalRevenue)
                .topProducts(topProducts)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderTrackingResponseDTO> getTracking(Long orderId) {
        return trackingRepository.findByOrder_IdOrderByChangedAtDesc(orderId).stream()
                .map(this::toTrackingDTO).collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────────────────────
    // STOCK AVAILABILITY SEARCH
    // ──────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<PharmacyStockResponseDTO> findPharmaciesWithProduct(Long productId, int minQty) {
        return stockRepository.findByProduct_IdAndTotalQuantityGreaterThan(productId, minQty).stream()
                .map(this::toStockDTO).collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ──────────────────────────────────────────────────────────────
    private void verifyPaymentCompleted(PharmacyOrder order) {
        Payment payment = paymentRepository.findByOrder_Id(order.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "❌ PAIEMENT MANQUANT: La commande #" + order.getId() +
                                " n'a pas encore été payée. Le patient doit d'abord effectuer le paiement."));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new IllegalStateException(
                    "❌ PAIEMENT NON CONFIRMÉ: Le statut du paiement est '" + payment.getStatus() +
                            "' pour la commande #" + order.getId() +
                            ". Seuls les paiements COMPLÉTÉS peuvent être livrés.");
        }
    }

    private void triggerDelivery(PharmacyOrder order) {
        DeliveryRequest req = DeliveryRequest.builder()
                .pickupAddress(order.getPharmacy().getAddress())
                .dropoffAddress(order.getDeliveryAddress())
                .packageDescription("Médicaments - Commande #" + order.getId())
                .externalOrderRef(order.getId().toString())
                .build();

        DeliveryResponse resp = deliveryGateway.createDelivery(req);

        Delivery delivery = Delivery.builder()
                .order(order)
                .agencyName(resp.getAgencyName())
                .externalTrackingId(resp.getTrackingId())
                .trackingUrl(resp.getTrackingUrl())
                .status(DeliveryStatus.REQUESTED)
                .estimatedArrival(resp.getEstimatedArrival())
                .requestedAt(LocalDateTime.now())
                .build();
        deliveryRepository.save(delivery);

        order.setStatus(PharmacyOrderStatus.ASSIGNING);
        addTracking(order, PharmacyOrderStatus.ASSIGNING,
                "Delivery requested from " + resp.getAgencyName() + " – tracking: " + resp.getTrackingId(),
                "SYSTEM");
    }

    private void addTracking(PharmacyOrder order, PharmacyOrderStatus status, String note, String changedBy) {
        OrderTracking tracking = OrderTracking.builder()
                .order(order)
                .status(status)
                .note(note)
                .changedBy(changedBy)
                .changedAt(LocalDateTime.now())
                .build();
        trackingRepository.save(tracking);
    }

    private void notifyUser(User user, PharmacyOrder order,
            NotificationType type, String title, String message) {
        if (user == null || user.getId() == null) {
            System.err.println("WARNING: Cannot send notification to null user for order #"
                    + (order != null ? order.getId() : "null"));
            return;
        }
        try {
            // Reload the user to avoid Hibernate JOINED-inheritance proxy issues
            User freshUser = userRepository.findById(user.getId()).orElse(user);
            Notification notif = Notification.builder()
                    .recipient(freshUser)
                    .order(order)
                    .type(type)
                    .title(title)
                    .message(message)
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            Notification saved = notificationRepository.save(notif);
            // 🚀 Send via WebSocket
            webSocketNotificationService.notifyUser(freshUser.getId(), saved);
        } catch (Exception e) {
            System.err.println("WARNING: Notification failed for user #" + user.getId() + ": " + e.getMessage());
            // Don't rethrow — notification failure should never break the core business
            // operation
        }
    }

    private void notifyPharmacy(Pharmacy pharmacy, PharmacyOrder order,
            NotificationType type, String title, String message) {
        if (pharmacy == null)
            return;
        // Find all pharmacists for this pharmacy
        List<Pharmacist> pharmacists = pharmacistRepository.findByPharmacy_Id(pharmacy.getId());

        if (pharmacists.isEmpty()) {
            System.err.println(
                    "WARNING: No pharmacists found for pharmacy #" + pharmacy.getId() + " - Notification not sent.");
        }

        for (Pharmacist p : pharmacists) {
            if (p == null)
                continue;
            Notification notif = Notification.builder()
                    .recipient(p)
                    .order(order)
                    .type(type)
                    .title(title)
                    .message(message)
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            Notification saved = notificationRepository.save(notif);

            // 🚀 Envoyer aussi via WebSocket au pharmacien connecté
            webSocketNotificationService.notifyUser(p.getId(), saved);
        }
    }

    private void sendStatusEmail(PharmacyOrder order, String status, String note) {
        try {
            User patient = order.getPatient();
            if (patient != null && patient.getEmail() != null) {
                emailService.sendOrderStatusUpdate(
                        patient.getEmail(),
                        patient.getFullName(),
                        order.getId(),
                        status,
                        note
                );
            }
        } catch (Exception e) {
            System.err.println("Email status update failed for order #" + order.getId() + ": " + e.getMessage());
        }
    }

    private PharmacyOrder findOrderOrThrow(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Patient not found with id: " + userId));
    }

    // ──────────────────────────────────────────────────────────────
    // DTO MAPPERS
    // ──────────────────────────────────────────────────────────────
    private PharmacyOrderResponseDTO toDTO(PharmacyOrder o) {
        return PharmacyOrderResponseDTO.builder()
                .id(o.getId())
                .patientId(o.getPatient() != null ? o.getPatient().getId() : null)
                .patientName(o.getPatient() != null ? o.getPatient().getFullName() : null)
                .pharmacyId(o.getPharmacy() != null ? o.getPharmacy().getId() : null)
                .pharmacyName(o.getPharmacy() != null ? o.getPharmacy().getName() : null)
                .pharmacyAddress(o.getPharmacy() != null ? o.getPharmacy().getAddress() : null)
                .prescriptionId(o.getPrescription() != null ? o.getPrescription().getId() : null)
                .status(o.getStatus())
                .totalPrice(o.getTotalPrice())
                .deliveryAddress(o.getDeliveryAddress())
                .scheduledDeliveryDate(o.getScheduledDeliveryDate())
                .prescriptionImageUrl(o.getPrescriptionImageUrl())
                .deliveryType(o.getDeliveryType())
                .estimatedDeliveryMin(o.getEstimatedDeliveryMin())
                .pharmacistNote(o.getPharmacistNote())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .items(o.getItems() == null ? List.of()
                        : o.getItems().stream().map(this::toItemDTO).collect(Collectors.toList()))
                .trackingHistory(o.getTrackingHistory() == null ? List.of()
                        : o.getTrackingHistory().stream().map(this::toTrackingDTO).collect(Collectors.toList()))
                .build();
    }

    private OrderItemResponseDTO toItemDTO(OrderItem i) {
        return OrderItemResponseDTO.builder()
                .id(i.getId())
                .productId(i.getProduct() != null ? i.getProduct().getId() : null)
                .productName(i.getProduct() != null ? i.getProduct().getName() : null)
                .quantity(i.getQuantity())
                .price(i.getPrice())
                .build();
    }

    private OrderTrackingResponseDTO toTrackingDTO(OrderTracking t) {
        return OrderTrackingResponseDTO.builder()
                .id(t.getId())
                .orderId(t.getOrder().getId())
                .status(t.getStatus())
                .note(t.getNote())
                .changedBy(t.getChangedBy())
                .changedAt(t.getChangedAt())
                .build();
    }

    private PharmacyStockResponseDTO toStockDTO(PharmacyStock s) {
        return PharmacyStockResponseDTO.builder()
                .id(s.getId())
                .pharmacyId(s.getPharmacy().getId())
                .pharmacyName(s.getPharmacy().getName())
                .productId(s.getProduct().getId())
                .productName(s.getProduct().getName())
                .totalQuantity(s.getTotalQuantity())
                .minQuantityThreshold(s.getMinQuantityThreshold())
                .unitPrice(s.getUnitPrice())
                .stockStatus(s.getTotalQuantity() <= s.getMinQuantityThreshold() ? "LOW" : "OK")
                .build();
    }


    @Override
    @Transactional(readOnly = true)
    public RouteResponseDTO getOrderRoute(Long orderId) {
        PharmacyOrder order = findOrderOrThrow(orderId);
        if (order.getDeliveryAddress() == null) return null;

        Pharmacy pharmacy = order.getPharmacy();
        double[] toCoords = geocodingService.getCoordinates(order.getDeliveryAddress());

        if (pharmacy.getLocationLat() != null && pharmacy.getLocationLng() != null) {
            return osrmService.getRouteByCoords(
                pharmacy.getLocationLat(), pharmacy.getLocationLng(),
                toCoords[0], toCoords[1]
            );
        } else {
            return osrmService.getRoute(pharmacy.getAddress(), order.getDeliveryAddress());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSalesStatsDTO> getProductSalesStats(Long pharmacyId) {
        return orderRepository.findProductSalesStatsByPharmacy(pharmacyId);
    }


    @Override
    @Transactional(readOnly = true)
    public List<PharmacyOrderResponseDTO> getByPharmacyNameAndStatus(String pharmacyName, PharmacyOrderStatus status) {
        return orderRepository.findByPharmacy_NameContainingIgnoreCaseAndStatus(pharmacyName, status)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addOrderItem(Long orderId, Long productId, int quantity) {
        PharmacyOrder order = findOrderOrThrow(orderId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));

        PharmacyStock stock = stockRepository.findByPharmacy_IdAndProduct_Id(order.getPharmacy().getId(), productId)
                .orElseThrow(() -> new IllegalStateException("Product not in pharmacy stock"));


        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setPrice(stock.getUnitPrice().multiply(BigDecimal.valueOf(quantity)));

        order.getItems().add(item);


        order.setTotalPrice(order.getTotalPrice().add(item.getPrice()));

        orderRepository.save(order);
    }

    // ── Advanced: JPQL Aging Report ───────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<OrderAgingDTO> getOrdersAging() {
        return orderRepository.findOrdersAging(
                List.of(PharmacyOrderStatus.PENDING, PharmacyOrderStatus.REVIEWING));
    }

    // ── Advanced: Escalation Scheduler ───────────────────────────
    @Override
    @Transactional
    public int escalateStalledOrders() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(1);
        List<PharmacyOrder> stalled = orderRepository
                .findByStatusAndCreatedAtBefore(PharmacyOrderStatus.PENDING, threshold);

        for (PharmacyOrder order : stalled) {
            order.setStatus(PharmacyOrderStatus.REVIEWING);
            order.setPharmacistNote(
                    "Auto-escalated: pending for over 1 hour without pharmacist review.");
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            addTracking(order, PharmacyOrderStatus.REVIEWING,
                    "Auto-escalated by system: order pending for over 1 hour.", "SYSTEM");

            notifyPharmacy(order.getPharmacy(), order, NotificationType.ORDER_CREATED,
                    "⚠️ Order Escalated #" + order.getId(),
                    "Order #" + order.getId() + " has been waiting for over 1 hour. Immediate review required.");
        }
        return stalled.size();
    }
}
