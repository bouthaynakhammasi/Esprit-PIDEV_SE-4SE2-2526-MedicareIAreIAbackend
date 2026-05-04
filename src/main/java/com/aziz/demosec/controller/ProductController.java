package com.aziz.demosec.controller;

import com.aziz.demosec.dto.ProductRequest;
import com.aziz.demosec.dto.ProductResponse;
import com.aziz.demosec.service.IProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final IProductService productService;
    private final com.aziz.demosec.repository.PharmacistRepository pharmacistRepository;

    private void checkPharmacistGuard(org.springframework.security.core.Authentication authentication) {
        if (authentication != null && authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PHARMACIST"))) {
            com.aziz.demosec.Entities.Pharmacist pharmacist = pharmacistRepository.findByEmail(authentication.getName())
                    .orElse(null);
            if (pharmacist != null && (!pharmacist.isPharmacySetupCompleted() || pharmacist.getStatus() != com.aziz.demosec.Entities.PharmacistStatus.APPROVED)) {
                throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "Your pharmacy is pending admin approval.");
            }
        }
    }

    @GetMapping
    public List<ProductResponse> getAll() {
        return productService.getAll();
    }

    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable Long id) {
        return productService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest request, org.springframework.security.core.Authentication authentication) {
        checkPharmacistGuard(authentication);
        return productService.create(request);
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id,
                                  @Valid @RequestBody ProductRequest request, org.springframework.security.core.Authentication authentication) {
        checkPharmacistGuard(authentication);
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, org.springframework.security.core.Authentication authentication) {
        checkPharmacistGuard(authentication);
        productService.delete(id);
    }
}