package com.aziz.demosec.service;

import com.aziz.demosec.dto.ProductRequest;
import com.aziz.demosec.dto.ProductResponse;
import com.aziz.demosec.Entities.Product;
import com.aziz.demosec.Entities.ProductType;
import com.aziz.demosec.Entities.ProductUnit;
import com.aziz.demosec.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductServiceImpl – Unit Tests")
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    // ─────────────────────────────────────────────────────────────────────────
    //  Helper
    // ─────────────────────────────────────────────────────────────────────────

    /** Returns a fully-populated ProductRequest */
    private ProductRequest buildRequest(String name) {
        ProductRequest req = new ProductRequest();
        req.setName(name);
        req.setDescription("Pain reliever");
        req.setManufacturer("PharmaCorp");
        req.setBrand("GenericBrand");
        req.setCategory("Analgesics");
        req.setType(ProductType.MEDICATION);
        req.setUnit(ProductUnit.PIECE);
        req.setBarcode("123456789");
        return req;
    }

    private Product buildProduct(Long id, String name) {
        return Product.builder()
                .id(id)
                .name(name)
                .description("Pain reliever")
                .manufacturer("PharmaCorp")
                .brand("GenericBrand")
                .category("Analgesics")
                .type(ProductType.MEDICATION)
                .unit(ProductUnit.PIECE)
                .barcode("123456789")
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CREATE
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("shouldCreateProductSuccessfully")
    void shouldCreateProductSuccessfully() {
        // GIVEN
        ProductRequest request = buildRequest("Paracetamol");
        Product saved = buildProduct(1L, "Paracetamol");

        when(productRepository.save(any(Product.class))).thenReturn(saved);

        // WHEN
        ProductResponse response = productService.create(request);

        // THEN
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Paracetamol", response.getName());
        assertEquals(ProductType.MEDICATION, response.getType());
        assertEquals(ProductUnit.PIECE, response.getUnit());

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertEquals("Paracetamol", captor.getValue().getName());
        assertEquals("PharmaCorp", captor.getValue().getManufacturer());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GET BY ID
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("shouldGetProductByIdSuccessfully")
    void shouldGetProductByIdSuccessfully() {
        // GIVEN
        Product product = buildProduct(5L, "Ibuprofen");
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));

        // WHEN
        ProductResponse response = productService.getById(5L);

        // THEN
        assertNotNull(response);
        assertEquals(5L, response.getId());
        assertEquals("Ibuprofen", response.getName());
        verify(productRepository).findById(5L);
    }

    @Test
    @DisplayName("shouldThrowExceptionWhenProductNotFound")
    void shouldThrowExceptionWhenProductNotFound() {
        // GIVEN
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // WHEN / THEN
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> productService.getById(999L));
        assertTrue(ex.getMessage().contains("999"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GET ALL
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("shouldReturnAllProducts")
    void shouldReturnAllProducts() {
        // GIVEN – build entities outside the when() call to avoid Mockito side-effect
        Product p1 = buildProduct(1L, "Paracetamol");
        Product p2 = buildProduct(2L, "Aspirin");
        Product p3 = buildProduct(3L, "Amoxicillin");
        when(productRepository.findAll()).thenReturn(List.of(p1, p2, p3));

        // WHEN
        List<ProductResponse> responses = productService.getAll();

        // THEN
        assertEquals(3, responses.size());
        verify(productRepository).findAll();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  UPDATE
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("shouldUpdateProductSuccessfully")
    void shouldUpdateProductSuccessfully() {
        // GIVEN
        Product existing = buildProduct(10L, "OldName");
        ProductRequest updateRequest = buildRequest("NewName");
        updateRequest.setManufacturer("NewCorp");

        when(productRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        // WHEN
        ProductResponse response = productService.update(10L, updateRequest);

        // THEN
        assertNotNull(response);
        assertEquals("NewName", response.getName());
        assertEquals("NewCorp", response.getManufacturer());

        verify(productRepository).findById(10L);
        verify(productRepository).save(existing);
    }

    @Test
    @DisplayName("shouldThrowExceptionWhenUpdatingNonExistentProduct")
    void shouldThrowExceptionWhenUpdatingNonExistentProduct() {
        // GIVEN
        when(productRepository.findById(404L)).thenReturn(Optional.empty());

        // WHEN / THEN
        assertThrows(EntityNotFoundException.class,
                () -> productService.update(404L, buildRequest("Test")));
        verify(productRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DELETE
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("shouldDeleteProductSuccessfully")
    void shouldDeleteProductSuccessfully() {
        // GIVEN
        when(productRepository.existsById(7L)).thenReturn(true);

        // WHEN
        productService.delete(7L);

        // THEN
        verify(productRepository).existsById(7L);
        verify(productRepository).deleteById(7L);
    }

    @Test
    @DisplayName("shouldThrowExceptionWhenDeletingNonExistentProduct")
    void shouldThrowExceptionWhenDeletingNonExistentProduct() {
        // GIVEN
        when(productRepository.existsById(999L)).thenReturn(false);

        // WHEN / THEN
        assertThrows(EntityNotFoundException.class, () -> productService.delete(999L));
        verify(productRepository, never()).deleteById(any());
    }
}
