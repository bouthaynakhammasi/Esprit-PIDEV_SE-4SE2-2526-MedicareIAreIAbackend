package com.aziz.demosec.Entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "online_events")
@DiscriminatorValue("ONLINE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class OnlineEvent extends MedicalEvent {

    private String platformName;
    private String meetingLink;
    private String meetingPassword;

}