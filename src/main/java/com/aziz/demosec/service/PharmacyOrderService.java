package com.aziz.demosec.service;

import com.aziz.demosec.dto.pharmacy.*;
import com.aziz.demosec.Entities.PharmacyOrderStatus;
import com.aziz.demosec.dto.pharmacy.OrderAgingDTO;

import java.util.List;

public interface PharmacyOrderService {
    PharmacyOrderResponseDTO createOrder(PharmacyOrderRequestDTO dto);

    PharmacyOrderResponseDTO updateStatus(Long orderId, UpdateOrderStatusRequestDTO dto);

    PharmacyOrderResponseDTO cancelOrder(Long orderId, String reason);

    PharmacyOrderResponseDTO rejectOrder(Long orderId, RejectOrderRequestDTO dto);

    PharmacyOrderResponseDTO getById(Long id);

    List<PharmacyOrderResponseDTO> getAll();

    List<PharmacyOrderResponseDTO> getByPatient(Long patientId);

    List<PharmacyOrderResponseDTO> getByPharmacy(Long pharmacyId);

    List<PharmacyOrderResponseDTO> getByPharmacyAndStatus(Long pharmacyId, String status);

    List<OrderTrackingResponseDTO> getTracking(Long orderId);

    List<PharmacyStockResponseDTO> findPharmaciesWithProduct(Long productId, int minQty);

    PharmacyStatsResponseDTO getPharmacyStats(Long pharmacyId);

    void addOrderItem(Long orderId, Long productId, int quantity);

        List<ProductSalesStatsDTO> getProductSalesStats(Long pharmacyId);


    List<PharmacyOrderResponseDTO> getByPharmacyNameAndStatus(String pharmacyName, PharmacyOrderStatus status);

    RouteResponseDTO getOrderRoute(Long orderId);

    // Advanced — JPQL aging report
    List<OrderAgingDTO> getOrdersAging();

    // Advanced — Escalation: PENDING > 1h → REVIEWING
    int escalateStalledOrders();
}
