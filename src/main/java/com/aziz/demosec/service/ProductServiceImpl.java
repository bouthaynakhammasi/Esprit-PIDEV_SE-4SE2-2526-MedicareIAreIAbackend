package com.aziz.demosec.service;

import com.aziz.demosec.Entities.Product;
import com.aziz.demosec.repository.ProductRepository;

import com.aziz.demosec.dto.ProductRequest;
import com.aziz.demosec.dto.ProductResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements IProductService {

    private final ProductRepository productRepository;

    @Override
    public ProductResponse create(ProductRequest request) {
        Product p = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .manufacturer(request.getManufacturer())
                .brand(request.getBrand())
                .category(request.getCategory())
                .type(request.getType())
                .barcode(request.getBarcode())
                .unit(request.getUnit())
                .build();

        return toResponse(productRepository.save(p));
    }

    @Override
    public ProductResponse update(Long id, ProductRequest request) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));

        p.setName(request.getName());
        p.setDescription(request.getDescription());
        p.setImageUrl(request.getImageUrl());
        p.setManufacturer(request.getManufacturer());
        p.setBrand(request.getBrand());
        p.setCategory(request.getCategory());
        p.setType(request.getType());
        p.setBarcode(request.getBarcode());
        p.setUnit(request.getUnit());

        return toResponse(productRepository.save(p));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return toResponse(productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAll() {
        return productRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new EntityNotFoundException("Product not found: " + id);
        }
        productRepository.deleteById(id);
    }

    private ProductResponse toResponse(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .imageUrl(p.getImageUrl())
                .manufacturer(p.getManufacturer())
                .brand(p.getBrand())
                .category(p.getCategory())
                .type(p.getType())
                .barcode(p.getBarcode())
                .unit(p.getUnit())
                .build();
    }
}