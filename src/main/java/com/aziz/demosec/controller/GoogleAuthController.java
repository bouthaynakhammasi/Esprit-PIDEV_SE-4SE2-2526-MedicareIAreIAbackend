package com.aziz.demosec.controller;

import com.aziz.demosec.dto.VerifyCodeRequest;
import com.aziz.demosec.dto.AuthResponse;
import com.aziz.demosec.security.jwt.JwtService;
import com.aziz.demosec.service.EmailVerificationService;
import com.aziz.demosec.service.PendingGoogleLoginService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/google")
@RequiredArgsConstructor
public class GoogleAuthController {

    private final EmailVerificationService emailService;
    private final PendingGoogleLoginService pending;
    private final JwtService jwtService;

    @PostMapping("/verify")
    public AuthResponse verify(@Valid @RequestBody VerifyCodeRequest req) {

        emailService.verify(req.getEmail(), req.getCode());

        String role = pending.get(req.getEmail());
        if (role == null || role.isBlank()) {
            role = "ROLE_PATIENT";
        }

        UserDetails user = User.withUsername(req.getEmail())
                .password("")
                .authorities(role)
                .build();

        String token = jwtService.generateToken(user, null, req.getEmail());

        pending.remove(req.getEmail());

        return new AuthResponse(token, req.getEmail(), req.getEmail(), role);
    }
}