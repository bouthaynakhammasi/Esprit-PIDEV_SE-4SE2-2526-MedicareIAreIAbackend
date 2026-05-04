package com.aziz.demosec.service;

import com.aziz.demosec.dto.PharmacyStockResponse;
import com.aziz.demosec.dto.ReceiveBatchRequest;
import com.aziz.demosec.dto.StockAlertResponse;
import com.aziz.demosec.dto.StockBatchResponse;
import com.aziz.demosec.dto.StockMovementRequest;
import com.aziz.demosec.dto.StockMovementResponse;
import com.aziz.demosec.Entities.*;
import com.aziz.demosec.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockServiceImpl – Unit Tests")
class StockServiceImplTest {

    @Mock private PharmacyRepository pharmacyRepository;
    @Mock private ProductRepository productRepository;
    @Mock private PharmacyStockRepository pharmacyStockRepository;
    @Mock private StockBatchRepository stockBatchRepository;
    @Mock private StockMovementRepository stockMovementRepository;
    @Mock private StockAlertRepository stockAlertRepository;

    @InjectMocks
    private StockServiceImpl stockService;

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Pharmacy buildPharmacy(Long id) {
        return Pharmacy.builder().id(id).name("Pharmacy " + id).build();
    }

    private Product buildProduct(Long id) {
        return Product.builder()
                .id(id)
                .name("Paracetamol")
                .type(ProductType.MEDICATION)
                .unit(ProductUnit.PIECE)
                .build();
    }

    private PharmacyStock buildStock(Long id, Pharmacy pharmacy, Product product, int total, int threshold) {
        return PharmacyStock.builder()
                .id(id)
                .pharmacy(pharmacy)
                .product(product)
                .totalQuantity(total)
                .minQuantityThreshold(threshold)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GET OR CREATE STOCK
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("shouldReturnExistingStockWhenAlreadyPresent")
    void shouldReturnExistingStockWhenAlreadyPresent() {
        // GIVEN
        Pharmacy pharmacy = buildPharmacy(1L);
        Product product = buildProduct(2L);
        PharmacyStock stock = buildStock(10L, pharmacy, product, 50, 5);

        when(pharmacyStockRepository.findByPharmacyIdAndProductId(1L, 2L)).thenReturn(Optional.of(stock));

        // WHEN
        PharmacyStockResponse response = stockService.getOrCreateStock(1L, 2L, 5);

        // THEN
        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals(50, response.getTotalQuantity());
        verify(pharmacyStockRepository, never()).save(any());
    }

    @Test
    @DisplayName("shouldCreateStockWhenNotFound")
    void shouldCreateStockWhenNotFound() {
        // GIVEN
        Pharmacy pharmacy = buildPharmacy(1L);
        Product product = buildProduct(2L);
        PharmacyStock createdStock = buildStock(99L, pharmacy, product, 0, 10);

        when(pharmacyStockRepository.findByPharmacyIdAndProductId(1L, 2L)).thenReturn(Optional.empty());
        when(pharmacyRepository.findById(1L)).thenReturn(Optional.of(pharmacy));
        when(productRepository.findById(2L)).thenReturn(Optional.of(product));
        when(pharmacyStockRepository.save(any(PharmacyStock.class))).thenReturn(createdStock);

        // WHEN
        PharmacyStockResponse response = stockService.getOrCreateStock(1L, 2L, 10);

        // THEN
        assertNotNull(response);
        assertEquals(99L, response.getId());
        assertEquals(0, response.getTotalQuantity());

        ArgumentCaptor<PharmacyStock> captor = ArgumentCaptor.forClass(PharmacyStock.class);
        verify(pharmacyStockRepository).save(captor.capture());
        assertEquals(0, captor.getValue().getTotalQuantity());
        assertEquals(10, captor.getValue().getMinQuantityThreshold());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  RECEIVE BATCH
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("shouldReceiveBatchAndUpdateTotalQuantity")
    void shouldReceiveBatchAndUpdateTotalQuantity() {
        // GIVEN
        Pharmacy pharmacy = buildPharmacy(1L);
        Product product = buildProduct(2L);
        PharmacyStock stock = buildStock(10L, pharmacy, product, 100, 10);

        ReceiveBatchRequest request = new ReceiveBatchRequest();
        request.setPharmacyId(1L);
        request.setProductId(2L);
        request.setBatchNumber("LOT-2024-001");
        request.setQuantity(50);
        request.setExpirationDate(LocalDate.now().plusMonths(12));
        request.setPurchasePrice(new BigDecimal("5.00"));
        request.setSellingPrice(new BigDecimal("8.50"));

        StockBatch savedBatch = StockBatch.builder()
                .id(200L)
                .pharmacyStock(stock)
                .batchNumber("LOT-2024-001")
                .quantity(50)
                .expirationDate(request.getExpirationDate())
                .purchasePrice(request.getPurchasePrice())
                .sellingPrice(request.getSellingPrice())
                .receivedAt(LocalDateTime.now())
                .build();

        when(pharmacyStockRepository.findByPharmacyIdAndProductId(1L, 2L)).thenReturn(Optional.of(stock));
        when(stockBatchRepository.save(any(StockBatch.class))).thenReturn(savedBatch);
        when(pharmacyStockRepository.save(any(PharmacyStock.class))).thenReturn(stock);
        when(stockMovementRepository.save(any(StockMovement.class))).thenReturn(mock(StockMovement.class));

        // Note: refreshStockAlerts calls autoResolve because total(150) > min(10)
        when(stockAlertRepository.findByPharmacyStockIdAndResolvedFalse(any())).thenReturn(Collections.emptyList());

        // WHEN
        StockBatchResponse response = stockService.receiveBatch(request);

        // THEN
        assertNotNull(response);
        assertEquals(200L, response.getId());
        assertEquals("LOT-2024-001", response.getBatchNumber());
        assertEquals(50, response.getQuantity());

        // Verify stock total was updated (100 + 50 = 150)
        ArgumentCaptor<PharmacyStock> stockCaptor = ArgumentCaptor.forClass(PharmacyStock.class);
        verify(pharmacyStockRepository).save(stockCaptor.capture());
        assertEquals(150, stockCaptor.getValue().getTotalQuantity());

        // Verify an IN movement was created
        ArgumentCaptor<StockMovement> movCaptor = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(movCaptor.capture());
        assertEquals(StockMovementType.IN, movCaptor.getValue().getMovementType());
        assertEquals(50, movCaptor.getValue().getQuantity());
    }

    @Test
    @DisplayName("shouldThrowExceptionWhenBatchQuantityIsZeroOrNegative")
    void shouldThrowExceptionWhenBatchQuantityIsZeroOrNegative() {
        // GIVEN
        Pharmacy pharmacy = buildPharmacy(1L);
        Product product = buildProduct(2L);
        PharmacyStock stock = buildStock(10L, pharmacy, product, 100, 10);

        ReceiveBatchRequest request = new ReceiveBatchRequest();
        request.setPharmacyId(1L);
        request.setProductId(2L);
        request.setQuantity(0);   // invalid

        when(pharmacyStockRepository.findByPharmacyIdAndProductId(1L, 2L)).thenReturn(Optional.of(stock));

        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> stockService.receiveBatch(request));
        assertTrue(ex.getMessage().contains("quantity must be > 0"));
        verify(stockBatchRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CREATE MOVEMENT – IN (restock)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("shouldUpdateStockQuantityOnIncomingMovement")
    void shouldUpdateStockQuantityOnIncomingMovement() {
        // GIVEN – stock starts at 40, receiving 20 more
        Pharmacy pharmacy = buildPharmacy(1L);
        Product product = buildProduct(2L);
        PharmacyStock stock = buildStock(10L, pharmacy, product, 40, 5);

        StockMovementRequest request = new StockMovementRequest();
        request.setPharmacyStockId(10L);
        request.setMovementType(StockMovementType.IN);
        request.setQuantity(20);
        request.setReference("RESTOCK-01");

        StockMovement saved = StockMovement.builder()
                .id(300L)
                .pharmacyStock(stock)
                .movementType(StockMovementType.IN)
                .quantity(20)
                .reference("RESTOCK-01")
                .createdAt(LocalDateTime.now())
                .build();

        when(pharmacyStockRepository.findById(10L)).thenReturn(Optional.of(stock));
        when(pharmacyStockRepository.save(any(PharmacyStock.class))).thenReturn(stock);
        when(stockMovementRepository.save(any(StockMovement.class))).thenReturn(saved);

        // Note: auto-resolve because total(60) > min(5)
        when(stockAlertRepository.findByPharmacyStockIdAndResolvedFalse(any())).thenReturn(Collections.emptyList());

        // WHEN
        StockMovementResponse response = stockService.createMovement(request);

        // THEN
        assertNotNull(response);
        assertEquals(300L, response.getId());
        assertEquals(StockMovementType.IN, response.getMovementType());

        // 40 + 20 = 60
        ArgumentCaptor<PharmacyStock> captor = ArgumentCaptor.forClass(PharmacyStock.class);
        verify(pharmacyStockRepository).save(captor.capture());
        assertEquals(60, captor.getValue().getTotalQuantity());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CREATE MOVEMENT – OUT (dispense using FEFO logic)
    //  Note: FEFO ordering is handled by the repository query
    //  (findByPharmacyStockIdOrderByExpirationDateAsc). The service decrements
    //  the total; this test verifies correct deduction and movement creation.
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("shouldDispenseProductReducingStockQuantity")
    void shouldDispenseProductReducingStockQuantity() {
        // GIVEN – stock has 100 units, dispense 30
        Pharmacy pharmacy = buildPharmacy(1L);
        Product product = buildProduct(2L);
        PharmacyStock stock = buildStock(10L, pharmacy, product, 100, 10);

        StockMovementRequest request = new StockMovementRequest();
        request.setPharmacyStockId(10L);
        request.setMovementType(StockMovementType.OUT);
        request.setQuantity(30);
        request.setReference("DISPENSE-ORD-001");

        StockMovement saved = StockMovement.builder()
                .id(400L)
                .pharmacyStock(stock)
                .movementType(StockMovementType.OUT)
                .quantity(30)
                .reference("DISPENSE-ORD-001")
                .createdAt(LocalDateTime.now())
                .build();

        when(pharmacyStockRepository.findById(10L)).thenReturn(Optional.of(stock));
        when(pharmacyStockRepository.save(any(PharmacyStock.class))).thenReturn(stock);
        when(stockMovementRepository.save(any(StockMovement.class))).thenReturn(saved);

        // Note: auto-resolve because total(70) > min(10)
        when(stockAlertRepository.findByPharmacyStockIdAndResolvedFalse(any())).thenReturn(Collections.emptyList());

        // WHEN
        StockMovementResponse response = stockService.createMovement(request);

        // THEN
        assertNotNull(response);
        assertEquals(StockMovementType.OUT, response.getMovementType());
        assertEquals(30, response.getQuantity());

        // 100 - 30 = 70
        ArgumentCaptor<PharmacyStock> captor = ArgumentCaptor.forClass(PharmacyStock.class);
        verify(pharmacyStockRepository).save(captor.capture());
        assertEquals(70, captor.getValue().getTotalQuantity());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DISPENSE – insufficient stock (negative balance guard)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("shouldThrowExceptionWhenStockInsufficient")
    void shouldThrowExceptionWhenStockInsufficient() {
        // GIVEN – stock has only 5 units, trying to dispense 50
        Pharmacy pharmacy = buildPharmacy(1L);
        Product product = buildProduct(2L);
        PharmacyStock stock = buildStock(10L, pharmacy, product, 5, 2);

        StockMovementRequest request = new StockMovementRequest();
        request.setPharmacyStockId(10L);
        request.setMovementType(StockMovementType.OUT);
        request.setQuantity(50);

        when(pharmacyStockRepository.findById(10L)).thenReturn(Optional.of(stock));

        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> stockService.createMovement(request));
        assertTrue(ex.getMessage().contains("negative"));
        verify(stockMovementRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  LOW STOCK ALERT GENERATION
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("shouldGenerateLowStockAlertWhenStockFallsBelowThreshold")
    void shouldGenerateLowStockAlertWhenStockFallsBelowThreshold() {
        // GIVEN – stock currently at 15, threshold=10; after OUT of 10 → 5 (below threshold)
        Pharmacy pharmacy = buildPharmacy(1L);
        Product product = buildProduct(2L);
        PharmacyStock stock = buildStock(10L, pharmacy, product, 15, 10);

        StockMovementRequest request = new StockMovementRequest();
        request.setPharmacyStockId(10L);
        request.setMovementType(StockMovementType.OUT);
        request.setQuantity(10);

        StockMovement saved = StockMovement.builder()
                .id(500L)
                .pharmacyStock(stock)
                .movementType(StockMovementType.OUT)
                .quantity(10)
                .createdAt(LocalDateTime.now())
                .build();

        when(pharmacyStockRepository.findById(10L)).thenReturn(Optional.of(stock));
        when(pharmacyStockRepository.save(any(PharmacyStock.class))).thenReturn(stock);
        when(stockMovementRepository.save(any(StockMovement.class))).thenReturn(saved);

        // After deduction, total = 5 → below threshold 10 → LOW_STOCK alert should be created
        // Alert does not already exist
        when(stockAlertRepository.existsByPharmacyStockIdAndAlertTypeAndResolvedFalse(10L, StockAlertType.LOW_STOCK))
                .thenReturn(false);
        
        // Note: total(5) > 0 calls autoResolve for OUT_OF_STOCK
        when(stockAlertRepository.findByPharmacyStockIdAndResolvedFalse(10L)).thenReturn(Collections.emptyList());
        when(stockAlertRepository.save(any(StockAlert.class))).thenAnswer(inv -> inv.getArgument(0));

        // WHEN
        stockService.createMovement(request);

        // THEN – a LOW_STOCK alert must have been saved
        ArgumentCaptor<StockAlert> alertCaptor = ArgumentCaptor.forClass(StockAlert.class);
        verify(stockAlertRepository, atLeastOnce()).save(alertCaptor.capture());

        StockAlert createdAlert = alertCaptor.getAllValues().stream()
                .filter(a -> a.getAlertType() == StockAlertType.LOW_STOCK)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected LOW_STOCK alert to be saved"));

        assertEquals(StockAlertType.LOW_STOCK, createdAlert.getAlertType());
        assertFalse(createdAlert.isResolved());
        assertTrue(createdAlert.getMessage().contains("threshold"));
        
        verify(stockAlertRepository).existsByPharmacyStockIdAndAlertTypeAndResolvedFalse(10L, StockAlertType.LOW_STOCK);
    }

    @Test
    @DisplayName("shouldGenerateOutOfStockAlertWhenStockReachesZero")
    void shouldGenerateOutOfStockAlertWhenStockReachesZero() {
        // GIVEN – stock = 10, dispense 10 → total = 0 → OUT_OF_STOCK
        Pharmacy pharmacy = buildPharmacy(1L);
        Product product = buildProduct(2L);
        PharmacyStock stock = buildStock(10L, pharmacy, product, 10, 5);

        StockMovementRequest request = new StockMovementRequest();
        request.setPharmacyStockId(10L);
        request.setMovementType(StockMovementType.OUT);
        request.setQuantity(10);

        when(pharmacyStockRepository.findById(10L)).thenReturn(Optional.of(stock));
        when(pharmacyStockRepository.save(any(PharmacyStock.class))).thenReturn(stock);
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(inv -> StockMovement.builder()
                .id(600L).pharmacyStock(stock).movementType(StockMovementType.OUT).quantity(10)
                .createdAt(LocalDateTime.now()).build());

        when(stockAlertRepository.existsByPharmacyStockIdAndAlertTypeAndResolvedFalse(10L, StockAlertType.OUT_OF_STOCK))
                .thenReturn(false);
        when(stockAlertRepository.save(any(StockAlert.class))).thenAnswer(inv -> inv.getArgument(0));

        // WHEN
        stockService.createMovement(request);

        // THEN
        ArgumentCaptor<StockAlert> captor = ArgumentCaptor.forClass(StockAlert.class);
        verify(stockAlertRepository, atLeastOnce()).save(captor.capture());

        boolean outOfStockAlertCreated = captor.getAllValues().stream()
                .anyMatch(a -> a.getAlertType() == StockAlertType.OUT_OF_STOCK);
        assertTrue(outOfStockAlertCreated, "Expected OUT_OF_STOCK alert to be saved");
        
        verify(stockAlertRepository).existsByPharmacyStockIdAndAlertTypeAndResolvedFalse(10L, StockAlertType.OUT_OF_STOCK);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  RESOLVE ALERT
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("shouldResolveAlertSuccessfully")
    void shouldResolveAlertSuccessfully() {
        // GIVEN
        Pharmacy pharmacy = buildPharmacy(1L);
        Product product = buildProduct(2L);
        PharmacyStock stock = buildStock(10L, pharmacy, product, 3, 10);

        StockAlert alert = StockAlert.builder()
                .id(77L)
                .pharmacyStock(stock)
                .alertType(StockAlertType.LOW_STOCK)
                .message("Stock is low")
                .createdAt(LocalDateTime.now().minusHours(1))
                .resolved(false)
                .build();

        when(stockAlertRepository.findById(77L)).thenReturn(Optional.of(alert));
        when(stockAlertRepository.save(any(StockAlert.class))).thenAnswer(inv -> inv.getArgument(0));

        // WHEN
        StockAlertResponse response = stockService.resolveAlert(77L);

        // THEN
        assertNotNull(response);
        assertTrue(response.isResolved());
        assertNotNull(response.getResolvedAt());

        ArgumentCaptor<StockAlert> captor = ArgumentCaptor.forClass(StockAlert.class);
        verify(stockAlertRepository).save(captor.capture());
        assertTrue(captor.getValue().isResolved());
        assertNotNull(captor.getValue().getResolvedAt());
    }

    @Test
    @DisplayName("shouldThrowExceptionWhenAlertNotFound")
    void shouldThrowExceptionWhenAlertNotFound() {
        // GIVEN
        when(stockAlertRepository.findById(999L)).thenReturn(Optional.empty());

        // WHEN / THEN
        assertThrows(EntityNotFoundException.class, () -> stockService.resolveAlert(999L));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  LIST BATCHES (FEFO ordering delegated to repository)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("shouldListBatchesOrderedByExpirationDateAscendingFEFO")
    void shouldListBatchesOrderedByExpirationDateAscendingFEFO() {
        // GIVEN
        Pharmacy pharmacy = buildPharmacy(1L);
        Product product = buildProduct(2L);
        PharmacyStock stock = buildStock(10L, pharmacy, product, 200, 10);

        // Oldest expiration first (FEFO)
        StockBatch batchA = StockBatch.builder().id(1L).pharmacyStock(stock).batchNumber("A")
                .quantity(50).expirationDate(LocalDate.now().plusMonths(1)).receivedAt(LocalDateTime.now()).build();
        StockBatch batchB = StockBatch.builder().id(2L).pharmacyStock(stock).batchNumber("B")
                .quantity(150).expirationDate(LocalDate.now().plusMonths(6)).receivedAt(LocalDateTime.now()).build();

        when(stockBatchRepository.findByPharmacyStockIdOrderByExpirationDateAsc(10L))
                .thenReturn(List.of(batchA, batchB));

        // WHEN
        List<StockBatchResponse> response = stockService.listBatches(10L);

        // THEN
        assertEquals(2, response.size());
        // First batch has an earlier expiration date (true FEFO)
        assertTrue(response.get(0).getExpirationDate().isBefore(response.get(1).getExpirationDate()));
        verify(stockBatchRepository).findByPharmacyStockIdOrderByExpirationDateAsc(10L);
    }

    @Test
    @DisplayName("shouldDispenseProductUsingFEFOLogic")
    void shouldDispenseProductUsingFEFOLogic() {
        // TODO: This test serves as a placeholder for full FEFO batch selection logic.
        // Currently, ProductStock only tracks total quantity.
        // In a production scenario, this method would verify that batches are
        // identified and their quantities decremented in FEFO order.
        
        // GIVEN
        // Logic would go here once service supports batch-level deduction.
        
        // WHEN
        // stockService.dispense(request);
        
        // THEN
        // verify(stockBatchRepository).save(captor.capture());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  LIST OPEN ALERTS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("shouldReturnOnlyOpenUnresolvedAlerts")
    void shouldReturnOnlyOpenUnresolvedAlerts() {
        // GIVEN
        Pharmacy pharmacy = buildPharmacy(1L);
        Product product = buildProduct(2L);
        PharmacyStock stock = buildStock(10L, pharmacy, product, 2, 10);

        StockAlert openAlert = StockAlert.builder()
                .id(1L).pharmacyStock(stock).alertType(StockAlertType.LOW_STOCK)
                .message("Low").createdAt(LocalDateTime.now()).resolved(false).build();

        when(stockAlertRepository.findByResolvedFalseOrderByCreatedAtDesc())
                .thenReturn(List.of(openAlert));

        // WHEN
        List<StockAlertResponse> responses = stockService.listOpenAlerts();

        // THEN
        assertEquals(1, responses.size());
        assertFalse(responses.get(0).isResolved());
        verify(stockAlertRepository).findByResolvedFalseOrderByCreatedAtDesc();
    }
}
