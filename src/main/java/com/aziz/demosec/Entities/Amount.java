package com.aziz.demosec.Entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Amount extends Donation {
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal money;
}