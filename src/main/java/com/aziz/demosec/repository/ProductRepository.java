package com.aziz.demosec.repository;

import com.aziz.demosec.Entities.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByBarcode(String barcode);
    boolean existsByNameIgnoreCase(String name);

}