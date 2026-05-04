package com.aziz.demosec.service;

import com.aziz.demosec.Entities.Treatment;
import com.aziz.demosec.Entities.TreatmentStatus;
import com.aziz.demosec.Entities.TreatmentType;
import com.aziz.demosec.Entities.*;
import com.aziz.demosec.repository.ConsultationRepository;
import com.aziz.demosec.repository.TreatmentRepository;
import com.aziz.demosec.dto.TreatmentRequest;
import com.aziz.demosec.dto.TreatmentResponse;
import com.aziz.demosec.Mapper.TreatmentMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class TreatmentService implements ITreatmentService {

    private TreatmentRepository treatmentRepository;
    private ConsultationRepository consultationRepository;
    private TreatmentMapper treatmentMapper;

    @Override
    @Transactional
    public TreatmentResponse addTreatment(TreatmentRequest request) {
        System.out.println("DEBUG: addTreatment called with " + request);

        if (request.getConsultationId() == null ||
                request.getTreatmentType() == null ||
                request.getDescription() == null ||
                request.getStatus() == null) {
            System.out.println("DEBUG: Missing required fields in TreatmentRequest");
            return null;
        }

        Consultation consultation = consultationRepository.findById(request.getConsultationId()).orElse(null);
        if (consultation == null) {
            System.out.println("DEBUG: Consultation NOT FOUND for ID " + request.getConsultationId());
            return null;
        }

        Treatment treatment = Treatment.builder()
                .consultation(consultation)
                .treatmentType(safeMapType(request.getTreatmentType()))
                .description(request.getDescription())
                .startDate(parseDate(request.getStartDate()))
                .endDate(parseDate(request.getEndDate()))
                .status(safeMapStatus(request.getStatus()))
                .build();

        Treatment saved = treatmentRepository.save(treatment);
        System.out.println("DEBUG: Treatment saved successfully with ID: " + saved.getId());
        return treatmentMapper.toDto(saved);
    }

    private TreatmentType safeMapType(String typeStr) {
        try {
            return TreatmentType.valueOf(typeStr.toUpperCase());
        } catch (Exception e) {
            System.out.println("DEBUG: Invalid TreatmentType '" + typeStr + "', falling back to MEDICATION");
            return TreatmentType.MEDICATION;
        }
    }

    private TreatmentStatus safeMapStatus(String statusStr) {
        try {
            if (statusStr.equalsIgnoreCase("ACTIVE")) return TreatmentStatus.ONGOING;
            return TreatmentStatus.valueOf(statusStr.toUpperCase());
        } catch (Exception e) {
            System.out.println("DEBUG: Invalid TreatmentStatus '" + statusStr + "', falling back to ONGOING");
            return TreatmentStatus.ONGOING;
        }
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            System.out.println("DEBUG: Failed to parse date '" + dateStr + "'");
            return null;
        }
    }

    @Override
    public TreatmentResponse selectTreatmentByIdWithGet(Long id) {
        Treatment t = treatmentRepository.findById(id).get();
        return treatmentMapper.toDto(t);
    }

    @Override
    public TreatmentResponse selectTreatmentByIdWithOrElse(Long id) {
        Treatment t = treatmentRepository.findById(id).orElse(null);
        if (t == null) return null;
        return treatmentMapper.toDto(t);
    }

    @Override
    public List<TreatmentResponse> selectAllTreatments() {
        List<Treatment> list = treatmentRepository.findAll();
        List<TreatmentResponse> responses = new ArrayList<>();
        for (Treatment t : list) {
            responses.add(treatmentMapper.toDto(t));
        }
        return responses;
    }

    @Override
    @Transactional
    public TreatmentResponse updateTreatment(Long id, TreatmentRequest request) {
        System.out.println("DEBUG: updateTreatment called for ID " + id);

        Treatment treatment = treatmentRepository.findById(id).orElse(null);
        if (treatment == null) {
            System.out.println("DEBUG: Treatment NOT FOUND for ID " + id);
            return null;
        }

        if (request.getConsultationId() != null) {
            Consultation consultation = consultationRepository.findById(request.getConsultationId()).orElse(null);
            if (consultation == null) {
                System.out.println("DEBUG: Consultation NOT FOUND for ID " + request.getConsultationId());
                return null;
            }
            treatment.setConsultation(consultation);
        }

        if (request.getTreatmentType() != null) treatment.setTreatmentType(safeMapType(request.getTreatmentType()));
        if (request.getDescription() != null) treatment.setDescription(request.getDescription());
        if (request.getStartDate() != null) treatment.setStartDate(parseDate(request.getStartDate()));
        if (request.getEndDate() != null) treatment.setEndDate(parseDate(request.getEndDate()));
        if (request.getStatus() != null) treatment.setStatus(safeMapStatus(request.getStatus()));

        Treatment updated = treatmentRepository.save(treatment);
        System.out.println("DEBUG: Treatment updated successfully");
        return treatmentMapper.toDto(updated);
    }

    @Override
    public void deleteTreatmentById(Long id) {
        treatmentRepository.deleteById(id);
    }

    @Override
    public void deleteAllTreatments() {
        treatmentRepository.deleteAll();
    }

    @Override
    public long countingTreatments() {
        return treatmentRepository.count();
    }

    @Override
    public boolean verifTreatmentById(Long id) {
        return treatmentRepository.existsById(id);
    }
}