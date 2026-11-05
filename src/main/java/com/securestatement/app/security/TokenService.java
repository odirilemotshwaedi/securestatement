package com.securestatement.app.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.Date;

public class TokenService {

    private static final String SECRET;
    private static final Algorithm ALGORITHM;

    static {
        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.isBlank()) {
            throw new ExceptionInInitializerError(
                new IllegalStateException(
                    "JWT_SECRET environment variable is not set. " +
                    "Set it to a non-empty string before starting the application."));
        }
        SECRET = secret;
        ALGORITHM = Algorithm.HMAC256(SECRET);
    }


    // Generate a token with customerId, fileId, and expiry in minutes
    public static String generateToken(String customerId, Long statementId, int expiryMinutes) {
        
        return JWT.create()
                .withClaim("customerId", customerId)
                .withClaim("statementId", statementId)
                .withExpiresAt(new Date(System.currentTimeMillis() + expiryMinutes * 60 * 1000))
                .sign(ALGORITHM);
    }

    // Validate and decode a token
    public static DecodedJWT validateToken(String token) {
        return JWT.require(ALGORITHM).build().verify(token);
    }
}