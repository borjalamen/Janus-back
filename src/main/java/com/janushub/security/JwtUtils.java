package com.janushub.security;

import com.janushub.model.User;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;         // <-- ¡IMPORTACIÓN CRÍTICA!
import org.slf4j.LoggerFactory;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);
    // Vamos a leer estos valores del 'application.properties'
    @Value("${janushub.jwt.secret}")
    private String jwtSecret;

    @Value("${janushub.jwt.expiration-ms}")
    private long jwtExpirationMs;

    // Genera el token JWT para un usuario
    public String generateJwtToken(User user) {
        // Almacenamos los roles y el nombre dentro del token
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", user.getRoles());
        claims.put("fullName", user.getFullName());

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(user.getUsername()) // El 'username' es el sujeto
            .setIssuedAt(new Date()) // Fecha de creación
            .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs)) // Fecha de expiración
            .signWith(key(), SignatureAlgorithm.HS512) // Firma
            .compact();
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parser().setSigningKey(key()).build()
                .parseSignedClaims(token).getPayload().getSubject();
    }

    // 3. Method to validate the token (Used by AuthTokenFilter)
    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser().setSigningKey(key()).build().parse(authToken);
            return true;
        } catch (io.jsonwebtoken.security.SignatureException e) {
            logger.error("Firma de JWT inválida: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("Token JWT mal formado: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("Token JWT expirado: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("Token JWT no soportado: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("Cadena de JWT vacía: {}", e.getMessage());
        }
        return false;
    }

    // Método privado para generar la clave de firma
    private Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }
}