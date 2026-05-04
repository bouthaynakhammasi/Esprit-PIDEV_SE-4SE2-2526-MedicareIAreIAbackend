package com.aziz.demosec.Entities;

import com.aziz.demosec.Entities.ProductType;
import com.aziz.demosec.Entities.ProductUnit;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    private String description;
    private String imageUrl;
    private String manufacturer;
    private String brand;
    private String category;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductType type;
    private String barcode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductUnit unit;
}
