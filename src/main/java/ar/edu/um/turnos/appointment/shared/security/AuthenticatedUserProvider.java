package ar.edu.um.turnos.appointment.shared.security;

import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUserProvider {

    public AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)
                || !authentication.isAuthenticated()) {
            throw new IllegalStateException("An authenticated end-user identity is required.");
        }

        String login = jwtAuthentication.getToken().getSubject();
        String publicIdClaim = jwtAuthentication.getToken().getClaimAsString(JwtClaims.PUBLIC_ID);
        UUID publicId = UUID.fromString(publicIdClaim);
        List<String> authorities = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .toList();

        return new AuthenticatedUser(login, publicId, authorities);
    }
}
