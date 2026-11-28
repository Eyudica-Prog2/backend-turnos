package ar.edu.um.turnos.appointment.user.infrastructure.web.dto;

import java.util.List;

/** OAuth2 token-introspection response consumed by backend-catalogo. */
public record TokenIntrospectionResponse(
        boolean active,
        String sub,
        String iss,
        Long exp,
        String publicId,
        String firstName,
        String lastName,
        List<String> authorities) {

    public static TokenIntrospectionResponse inactive() {
        return new TokenIntrospectionResponse(false, null, null, null, null, null, null, List.of());
    }
}
