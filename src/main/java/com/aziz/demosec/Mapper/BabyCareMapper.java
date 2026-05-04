package com.aziz.demosec.Mapper;

import com.aziz.demosec.Entities.DiaperRecord;
import com.aziz.demosec.Entities.JournalEntry;
import com.aziz.demosec.Entities.ParentPreference;
import com.aziz.demosec.Entities.Vaccination;
import com.aziz.demosec.Entities.*;
import com.aziz.demosec.dto.baby.*;
import org.springframework.stereotype.Component;
import java.util.stream.Collectors;

@Component
public class BabyCareMapper {

    public BabyProfileResponseDTO toResponseDTO(BabyProfile baby) {
        if (baby == null) return null;
        return BabyProfileResponseDTO.builder()
                .id(baby.getId())
                .patientId(baby.getParent() != null ? baby.getParent().getId() : null)
                .name(baby.getName())
                .birthDate(baby.getBirthDate())
                .gender(baby.getGender().toString())
                .birthWeight(baby.getBirthWeight())
                .birthHeight(baby.getBirthHeight())
                .photoUrl(baby.getPhotoUrl())
                .createdAt(baby.getCreatedAt())
                .updatedAt(baby.getUpdatedAt())
                .preferences(baby.getPreferences() != null ? 
                    baby.getPreferences().stream().map(this::toPreferenceDTO).collect(Collectors.toList()) : null)
                .build();
    }

    public ParentPreferenceDTO toPreferenceDTO(ParentPreference pref) {
        if (pref == null) return null;
        return ParentPreferenceDTO.builder()
                .id(pref.getId())
                .priorityType(pref.getPriorityType())
                .selected(pref.isSelected())
                .build();
    }

    public VaccineSummaryDTO toVaccineResponseDTO(Vaccination v) {
        if (v == null) return null;
        return VaccineSummaryDTO.builder()
                .id(v.getId())
                .name(v.getVaccineName())
                .description(v.getDescription())
                .dueDate(v.getAdministeredDate()) 
                .status("DONE")
                .build();
    }

    public JournalEntryResponseDTO toJournalResponseDTO(JournalEntry j) {
        if (j == null) return null;
        return JournalEntryResponseDTO.builder()
                .id(j.getId())
                .babyProfileId(j.getBabyProfile() != null ? j.getBabyProfile().getId() : null)
                .entryType(j.getEntryType().toString())
                .value(j.getValue())
                .notes(j.getNotes())
                .metadata(j.getMetadata())
                .createdAt(j.getCreatedAt())
                .build();
    }

    public DiaperRecordDTO toDiaperRecordDTO(DiaperRecord d) {
        if (d == null) return null;
        return DiaperRecordDTO.builder()
                .id(d.getId())
                .babyId(d.getBabyProfile() != null ? d.getBabyProfile().getId() : null)
                .diaperType(d.getDiaperType().toString())
                .rashNoted(d.isRashNoted())
                .stoolColor(d.getStoolColor())
                .stoolTexture(d.getStoolTexture())
                .notes(d.getNotes())
                .changedAt(d.getChangedAt())
                .build();
    }
}
