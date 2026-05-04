
package com.aziz.demosec.dto;

import com.aziz.demosec.Entities.MedicalEventType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MedicalEventResponse {

    private Long id;
    private String title;
    private String description;
    private LocalDateTime date;
    private MedicalEventType eventType;

    private Long createdById;
    private String imageUrl;

    // PHYSICAL
    private String venueName;
    private String address;
    private String city;
    private String postalCode;
    private String country;
    private Integer capacity;
    private Double ticketPrice;
    /** Venue layout type for seating auto-generation (HOTEL/STADIUM/CONFERENCE). Null for ONLINE. */
    private String venueType;

    // ONLINE
    private String platformName;
    private String meetingLink;
    private String meetingPassword;
}
