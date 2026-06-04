package com.tn.gias.transport.controller;

import com.tn.gias.transport.entity.Utilisateur;
import com.tn.gias.transport.security.JwtUtil;
import com.tn.gias.transport.service.UtilisateurService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UtilisateurService service;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UtilisateurService service, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.service = service;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;

    }

    @PostMapping("/register")
    public Utilisateur register(@RequestBody Utilisateur user) {
        return service.save(user);
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Utilisateur user) {

        Utilisateur u = service.findByEmail(user.getEmail());

        if (u == null) {
            throw new RuntimeException("User not found");
        }

        if (!passwordEncoder.matches(user.getMotDePasse(), u.getMotDePasse())) {
            throw new RuntimeException("Mot de passe incorrect");
        }

        // 🔥 هنا نبعث role
        String token = jwtUtil.generateToken(u.getEmail(), u.getRole().name());

        return Map.of("token", token);
    }
}
