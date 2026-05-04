package com.aziz.demosec.service;

import com.aziz.demosec.Entities.Pharmacy;
import com.aziz.demosec.repository.PharmacyRepository;
import com.aziz.demosec.dto.PharmacyRequest;
import com.aziz.demosec.dto.PharmacyResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PharmacyServiceImpl implements IPharmacyService {

    private final PharmacyRepository pharmacyRepository;

    @Override
    public PharmacyResponse create(PharmacyRequest request) {
        Pharmacy pharmacy = Pharmacy.builder()
                .name(request.getName())
                .address(request.getAddress())
                .locationLat(request.getLocationLat())
                .locationLng(request.getLocationLng())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .build();

        return toResponse(pharmacyRepository.save(pharmacy));
    }

    @Override
    public PharmacyResponse update(Long id, PharmacyRequest request) {
        Pharmacy pharmacy = pharmacyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pharmacy not found: " + id));

        pharmacy.setName(request.getName());
        pharmacy.setAddress(request.getAddress());
        pharmacy.setLocationLat(request.getLocationLat());
        pharmacy.setLocationLng(request.getLocationLng());
        pharmacy.setPhoneNumber(request.getPhoneNumber());
        pharmacy.setEmail(request.getEmail());

        return toResponse(pharmacyRepository.save(pharmacy));
    }

    @Override
    @Transactional(readOnly = true)
    public PharmacyResponse getById(Long id) {
        Pharmacy pharmacy = pharmacyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pharmacy not found: " + id));
        return toResponse(pharmacy);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PharmacyResponse> getAll() {
        return pharmacyRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public void delete(Long id) {
        if (!pharmacyRepository.existsById(id)) {
            throw new EntityNotFoundException("Pharmacy not found: " + id);
        }
        pharmacyRepository.deleteById(id);
    }

    private PharmacyResponse toResponse(Pharmacy p) {
        return PharmacyResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .address(p.getAddress())
                .locationLat(p.getLocationLat())
                .locationLng(p.getLocationLng())
                .phoneNumber(p.getPhoneNumber())
                .email(p.getEmail())
                .build();
    }
}
