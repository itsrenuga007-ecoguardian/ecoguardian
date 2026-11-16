
package com.ecoguardian.ecoguardian;

import java.util.Map;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public record RegisterRequest(
            String name,
            String email,
            String password,
            String confirmPassword) {
    }

    public record LoginRequest(String email, String password) {
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
            @RequestBody RegisterRequest request) {

        if (request.name() == null || request.name().isBlank()
                || request.email() == null || request.email().isBlank()
                || request.password() == null
                || request.confirmPassword() == null) {
            return error(HttpStatus.BAD_REQUEST,
                    "Please fill in all fields.");
        }

        String name = request.name().trim();
        String email = request.email().trim().toLowerCase();

        if (name.length() > 100 || email.length() > 150) {
            return error(HttpStatus.BAD_REQUEST,
                    "Name or email is too long.");
        }

        if (!Pattern.matches(
                "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
                email)) {
            return error(HttpStatus.BAD_REQUEST,
                    "Please enter a valid email address.");
        }

        if (request.password().length() < 8
                || request.password().length() > 72) {
            return error(HttpStatus.BAD_REQUEST,
                    "Password must contain 8 to 72 characters.");
        }

        if (!request.password().equals(request.confirmPassword())) {
            return error(HttpStatus.BAD_REQUEST,
                    "Passwords do not match.");
        }

        if (userRepository.existsByEmail(email)) {
            return error(HttpStatus.CONFLICT,
                    "This email is already registered.");
        }

        AppUser user = new AppUser();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole("USER");

        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message",
                        "Account created successfully. Please sign in."));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(
            @RequestBody LoginRequest request,
            HttpSession session) {

        if (request.email() == null || request.email().isBlank()
                || request.password() == null
                || request.password().isBlank()) {
            return error(HttpStatus.BAD_REQUEST,
                    "Please enter your email and password.");
        }

        String email = request.email().trim().toLowerCase();

        AppUser user = userRepository.findByEmail(email).orElse(null);

        if (user == null
                || !passwordEncoder.matches(
                        request.password(), user.getPasswordHash())) {
            return error(HttpStatus.UNAUTHORIZED,
                    "Invalid email or password.");
        }

        // Store only the authenticated user's ID and role in the session.
        session.setAttribute("userId", user.getId());
        session.setAttribute("userRole", user.getRole());
        session.setAttribute("userName", user.getName());

        return ResponseEntity.ok(Map.of(
                "message", "Login successful.",
                "role", user.getRole(),
                "name", user.getName()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            HttpSession session) {

        session.invalidate();

        return ResponseEntity.ok(
                Map.of("message", "Logged out successfully."));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> currentUser(
            HttpSession session) {

        Object userId = session.getAttribute("userId");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Please sign in."));
        }

        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "role", session.getAttribute("userRole"),
                "name", session.getAttribute("userName")
        ));
    }

    private ResponseEntity<Map<String, String>> error(
            HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(Map.of("message", message));
    }
}
