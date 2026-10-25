package com.atharv.resumefit.user;

import com.atharv.resumefit.common.ConflictException;
import com.atharv.resumefit.common.UnauthorizedException;
import com.atharv.resumefit.security.JwtService;
import com.atharv.resumefit.user.AuthDtos.AuthResponse;
import com.atharv.resumefit.user.AuthDtos.LoginRequest;
import com.atharv.resumefit.user.AuthDtos.MeResponse;
import com.atharv.resumefit.user.AuthDtos.RegisterRequest;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = normalize(req.email());
        if (users.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        User user = users.save(new User(email, encoder.encode(req.password()), req.fullName().strip()));
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmail(normalize(req.email()))
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public MeResponse me(String email) {
        User user = users.findByEmail(email).orElseThrow(() -> new UnauthorizedException("Account no longer exists"));
        return new MeResponse(user.getEmail(), user.getFullName());
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(jwt.generate(user.getEmail()), user.getEmail(), user.getFullName(), jwt.expirySeconds());
    }

    private static String normalize(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
