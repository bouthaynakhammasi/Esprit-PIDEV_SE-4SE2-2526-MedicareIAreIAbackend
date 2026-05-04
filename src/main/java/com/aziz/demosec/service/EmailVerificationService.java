package com.aziz.demosec.service;

import com.aziz.demosec.Entities.EmailVerificationCode;
import com.aziz.demosec.repository.EmailVerificationCodeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationCodeRepository repo;
    private final MailService mailService;

    public void sendCode(String email) {
        String code = generateCode();

        System.out.println("🔐 Generated code: " + code);

        EmailVerificationCode entity = EmailVerificationCode.builder()
                .email(email)
                .code(code)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();

        repo.save(entity);

        mailService.sendCode(email, code);
    }

    public void verify(String email, String code) {
        EmailVerificationCode entity = repo.findTopByEmailOrderByIdDesc(email)
                .orElseThrow(() -> new EntityNotFoundException("No verification code found for email: " + email));

        if (entity.isUsed()) {
            throw new RuntimeException("Code already used");
        }

        if (entity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Code expired");
        }

        if (!entity.getCode().equals(code)) {
            throw new RuntimeException("Invalid verification code");
        }

        entity.setUsed(true);
        repo.save(entity);
    }

    private String generateCode() {
        SecureRandom random = new SecureRandom();
        int number = 100000 + random.nextInt(900000);
        return String.valueOf(number);
    }
}