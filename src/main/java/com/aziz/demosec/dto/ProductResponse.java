package com.aziz.demosec.dto;

import com.aziz.demosec.Entities.ProductType;
import com.aziz.demosec.Entities.ProductUnit;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private String manufacturer;
    private String brand;
    private String category;
    private ProductType type;
    private String barcode;
    private ProductUnit unit;
}