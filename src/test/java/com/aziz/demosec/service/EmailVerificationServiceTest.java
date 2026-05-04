package com.aziz.demosec.service;

import com.aziz.demosec.Entities.EmailVerificationCode;
import com.aziz.demosec.repository.EmailVerificationCodeRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailVerificationService (OTP) – Unit Tests")
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationCodeRepository repo;

    @Mock
    private MailService mailService;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    // ─────────────────────────────────────────────────────────────────────────
    //  Helper
    // ─────────────────────────────────────────────────────────────────────────

    private EmailVerificationCode buildCode(String email, String code,
                                            boolean used, LocalDateTime expiresAt) {
        return EmailVerificationCode.builder()
                .id(1L)
                .email(email)
                .code(code)
                .used(used)
                .expiresAt(expiresAt)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SEND CODE
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("shouldSendOtpCodeAndPersistIt")
    void shouldSendOtpCodeAndPersistIt() {
        // GIVEN
        String email = "user@test.com";
        when(repo.save(any(EmailVerificationCode.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // WHEN
        emailVerificationService.sendCode(email);

        // THEN
        ArgumentCaptor<EmailVerificationCode> captor = ArgumentCaptor.forClass(EmailVerificationCode.class);
        verify(repo).save(captor.capture());

        EmailVerificationCode saved = captor.getValue();
        assertEquals(email, saved.getEmail());
        assertNotNull(saved.getCode());
        assertEquals(6, saved.getCode().length());   // 6-digit OTP
        assertFalse(saved.isUsed());
        assertTrue(saved.getExpiresAt().isAfter(LocalDateTime.now()));

        // Mail was sent
        verify(mailService).sendCode(eq(email), anyString());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  VERIFY – success
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("shouldVerifyOtpSuccessfully")
    void shouldVerifyOtpSuccessfully() {
        // GIVEN
        String email = "user@test.com";
        String code  = "482931";

        EmailVerificationCode entity = buildCode(email, code, false,
                LocalDateTime.now().plusMinutes(5));   // valid, not expired

        when(repo.findTopByEmailOrderByIdDesc(email)).thenReturn(Optional.of(entity));
        when(repo.save(any(EmailVerificationCode.class))).thenAnswer(inv -> inv.getArgument(0));

        // WHEN
        emailVerificationService.verify(email, code);   // should NOT throw

        // THEN – code marked as used
        ArgumentCaptor<EmailVerificationCode> captor = ArgumentCaptor.forClass(EmailVerificationCode.class);
        verify(repo).save(captor.capture());
        assertTrue(captor.getValue().isUsed());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  VERIFY – fail: no code on record
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("shouldThrowExceptionWhenNoOtpFoundForEmail")
    void shouldThrowExceptionWhenNoOtpFoundForEmail() {
        // GIVEN
        when(repo.findTopByEmailOrderByIdDesc("unknown@test.com")).thenReturn(Optional.empty());

        // WHEN / THEN
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> emailVerificationService.verify("unknown@test.com", "999999"));
        assertTrue(ex.getMessage().contains("unknown@test.com"));
        verify(repo, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  VERIFY – fail: code already used
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("shouldThrowExceptionWhenOtpAlreadyUsed")
    void shouldThrowExceptionWhenOtpAlreadyUsed() {
        // GIVEN
        String email = "user@test.com";
        EmailVerificationCode entity = buildCode(email, "123456", true,
                LocalDateTime.now().plusMinutes(5));   // used = true

        when(repo.findTopByEmailOrderByIdDesc(email)).thenReturn(Optional.of(entity));

        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> emailVerificationService.verify(email, "123456"));
        assertTrue(ex.getMessage().contains("already used"));
        verify(repo, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  VERIFY – fail: code expired
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("shouldThrowExceptionWhenOtpIsExpired")
    void shouldThrowExceptionWhenOtpIsExpired() {
        // GIVEN
        String email = "user@test.com";
        EmailVerificationCode entity = buildCode(email, "654321", false,
                LocalDateTime.now().minusMinutes(15));   // expired 15 min ago

        when(repo.findTopByEmailOrderByIdDesc(email)).thenReturn(Optional.of(entity));

        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> emailVerificationService.verify(email, "654321"));
        assertTrue(ex.getMessage().contains("expired"));
        verify(repo, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  VERIFY – fail: wrong code
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("shouldThrowExceptionWhenOtpInvalid")
    void shouldThrowExceptionWhenOtpInvalid() {
        // GIVEN
        String email = "user@test.com";
        EmailVerificationCode entity = buildCode(email, "111111", false,
                LocalDateTime.now().plusMinutes(10));

        when(repo.findTopByEmailOrderByIdDesc(email)).thenReturn(Optional.of(entity));

        // WHEN / THEN – provide wrong code
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> emailVerificationService.verify(email, "999999"));
        assertTrue(ex.getMessage().contains("Invalid"));
        verify(repo, never()).save(any());
    }
}
