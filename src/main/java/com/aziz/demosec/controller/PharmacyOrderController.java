package com.aziz.demosec.controller;

import com.aziz.demosec.Entities.PharmacyOrderStatus;
import com.aziz.demosec.dto.pharmacy.*;
import java.time.LocalDateTime;
import java.util.Map;
import com.aziz.demosec.service.InvoiceService;
import com.aziz.demosec.service.OsrmService;
import com.aziz.demosec.service.PharmacyOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pharmacy/orders")
@RequiredArgsConstructor
public class PharmacyOrderController {

    private final PharmacyOrderService orderService;
    private final InvoiceService invoiceService;
    private final OsrmService osrmService;

    @PostMapping
    public ResponseEntity<PharmacyOrderResponseDTO> create(
            @Valid @RequestBody PharmacyOrderRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(dto));
    }

    @GetMapping
    public ResponseEntity<List<PharmacyOrderResponseDTO>> getAll() {
        return ResponseEntity.ok(orderService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PharmacyOrderResponseDTO> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(orderService.getById(id));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<PharmacyOrderResponseDTO>> getByPatient(@PathVariable("patientId") Long patientId) {
        return ResponseEntity.ok(orderService.getByPatient(patientId));
    }

    @GetMapping("/pharmacy/{pharmacyId}")
    public ResponseEntity<List<PharmacyOrderResponseDTO>> getByPharmacy(@PathVariable("pharmacyId") Long pharmacyId) {
        System.out.println("DEBUG: Fetching orders for pharmacy ID: " + pharmacyId);
        return ResponseEntity.ok(orderService.getByPharmacy(pharmacyId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<PharmacyOrderResponseDTO> updateStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateOrderStatusRequestDTO dto) {
        return ResponseEntity.ok(orderService.updateStatus(id, dto));
    }

    @GetMapping("/{id}/tracking")
    public ResponseEntity<List<OrderTrackingResponseDTO>> getTracking(@PathVariable("id") Long id) {
        return ResponseEntity.ok(orderService.getTracking(id));
    }

    @GetMapping("/stock/search")
    public ResponseEntity<List<PharmacyStockResponseDTO>> findPharmaciesWithProduct(
            @RequestParam("productId") Long productId,
            @RequestParam(value = "minQty", defaultValue = "0") int minQty) {
        return ResponseEntity.ok(orderService.findPharmaciesWithProduct(productId, minQty));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<PharmacyOrderResponseDTO> cancelOrder(
            @PathVariable("id") Long id,
            @Valid @RequestBody CancelOrderRequestDTO dto) {
        return ResponseEntity.ok(orderService.cancelOrder(id, dto.getReason()));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<PharmacyOrderResponseDTO> rejectOrder(
            @PathVariable("id") Long id,
            @Valid @RequestBody RejectOrderRequestDTO dto) {
        return ResponseEntity.ok(orderService.rejectOrder(id, dto));
    }

    @GetMapping("/pharmacy/{pharmacyId}/filter")
    public ResponseEntity<List<PharmacyOrderResponseDTO>> getByPharmacyAndStatus(
            @PathVariable("pharmacyId") Long pharmacyId,
            @RequestParam("status") String status) {
        return ResponseEntity.ok(orderService.getByPharmacyAndStatus(pharmacyId, status));
    }

    @GetMapping("/pharmacy/{pharmacyId}/stats")
    public ResponseEntity<PharmacyStatsResponseDTO> getStats(
            @PathVariable("pharmacyId") Long pharmacyId) {
        return ResponseEntity.ok(orderService.getPharmacyStats(pharmacyId));
    }

    // Task 2 – Stats ventes par produit pour une pharmacie (JPQL jointures)
    @GetMapping("/pharmacy/{pharmacyId}/product-sales")
    public ResponseEntity<List<ProductSalesStatsDTO>> getProductSalesStats(
            @PathVariable("pharmacyId") Long pharmacyId) {
        return ResponseEntity.ok(orderService.getProductSalesStats(pharmacyId));
    }

    // Task 3 – Commandes par nom de pharmacie + statut (keyword Spring Data 2 tables)
    @GetMapping("/search")
    public ResponseEntity<List<PharmacyOrderResponseDTO>> getByPharmacyNameAndStatus(
            @RequestParam("pharmacyName") String pharmacyName,
            @RequestParam("status") PharmacyOrderStatus status) {
        return ResponseEntity.ok(orderService.getByPharmacyNameAndStatus(pharmacyName, status));
    }

    @GetMapping("/{id}/route")
    public ResponseEntity<RouteResponseDTO> getRoute(@PathVariable("id") Long id) {
        PharmacyOrderResponseDTO order = orderService.getById(id);
        if (order.getDeliveryType() == null || !order.getDeliveryType().toString().equals("HOME_DELIVERY")) {
            return ResponseEntity.badRequest().build();
        }
        RouteResponseDTO route = orderService.getOrderRoute(id);
        if (route == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(route);
    }

    // Advanced — JPQL Aging Report
    @GetMapping("/admin/orders-aging")
    public ResponseEntity<List<OrderAgingDTO>> getOrdersAging() {
        return ResponseEntity.ok(orderService.getOrdersAging());
    }

    // Advanced — Manual escalation trigger (for demo)
    @PostMapping("/admin/trigger-escalation")
    public ResponseEntity<Map<String, Object>> triggerEscalation() {
        int count = orderService.escalateStalledOrders();
        return ResponseEntity.ok(Map.of(
                "escalated", count,
                "message", count + " order(s) escalated from PENDING to REVIEWING",
                "triggeredAt", LocalDateTime.now().toString()
        ));
    }

    @GetMapping("/{id}/invoice")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable("id") Long id) {
        byte[] pdfBytes = invoiceService.generateInvoice(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=\"invoice-order-" + id + ".pdf\"")
                .body(pdfBytes);
    }
}
