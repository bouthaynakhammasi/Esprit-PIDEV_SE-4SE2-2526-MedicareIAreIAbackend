package com.aziz.demosec.Entities;

import com.aziz.demosec.domain.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "event_seats", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"event_id", "seat_label"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private MedicalEvent event;

    @Column(name = "zone_name")
    private String zoneName; // e.g. "Zone A", "VIP", "Balcony"

    @Column(name = "seat_label", nullable = false)
    private String seatLabel; // e.g. "A-12"

    @Column(name = "pos_x")
    private Double posX;

    @Column(name = "pos_y")
    private Double posY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status;

    /** Row number within a section/zone (STADIUM, CONFERENCE). Null for HOTEL. */
    @Column(name = "row_number")
    private Integer rowNumber;

    /** Seat number within a row or around a table. */
    @Column(name = "seat_number")
    private Integer seatNumber;

    /** Table number (HOTEL only). Null for STADIUM / CONFERENCE. */
    @Column(name = "table_number")
    private Integer tableNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserved_by_id")
    private User reservedBy;
}
