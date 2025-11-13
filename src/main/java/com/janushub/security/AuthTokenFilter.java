package com.janushub.security;
import java.io.IOException;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.janushub.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // 1. Obtener el token JWT del encabezado de la petición
            String jwt = parseJwt(request);

            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                
                // 2. Obtener el nombre de usuario del token
                String username = jwtUtils.getUserNameFromJwtToken(jwt);

                // 3. Cargar los detalles del usuario desde la base de datos
                // Nota: Asume que UserRepository devuelve null si no lo encuentra.
                // En un proyecto real, usarías un UserDetailsService.
                var user = userRepository.findByUsername(username);

                if (user != null) {
                    // 4. Crear un token de autenticación para Spring Security
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            user, 
                            null, 
                            Collections.emptyList() // Usamos una lista vacía ya que las roles/autoridades se manejan después.
                    );

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 5. Establecer la autenticación en el Contexto de Seguridad
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception e) {
            logger.error("No se pudo establecer la autenticación del usuario: {}", e.getMessage());
        }

        // Continuar con el resto de la cadena de filtros de Spring Security
        filterChain.doFilter(request, response);
    }

    /**
     * Extrae la parte del token JWT (después de "Bearer ") del encabezado.
     */
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            // Devolver la cadena JWT sin "Bearer "
            return headerAuth.substring(7);
        }

        return null;
    }
}
