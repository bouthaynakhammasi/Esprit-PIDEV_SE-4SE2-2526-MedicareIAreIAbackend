package com.aziz.demosec.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    String fullName;

    @Column(nullable = false, unique = true)
    String email;

    @Column(nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(50)")
    Role role;

    String phone;

    LocalDate birthDate;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    String photo;

    String professionalDocument;

    @Builder.Default
    boolean enabled = true;

    @Builder.Default
    boolean profileCompleted = false;

    @Column(columnDefinition = "LONGTEXT")
    String profileImage;

    public String getProfileImage() { return this.profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }

}
