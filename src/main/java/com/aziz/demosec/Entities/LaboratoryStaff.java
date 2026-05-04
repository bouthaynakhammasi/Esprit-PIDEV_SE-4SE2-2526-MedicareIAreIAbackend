package com.aziz.demosec.Entities;

import com.aziz.demosec.domain.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@DiscriminatorValue("LaboratoryStaff")

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LaboratoryStaff extends User {

    @ManyToOne
    @JoinColumn(name = "laboratory_id")
    private Laboratory laboratory;

}
