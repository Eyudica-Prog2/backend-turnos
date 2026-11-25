package ar.edu.um.turnos.appointment.shared.security;

import java.util.List;
import java.util.UUID;

/** Identity and roles from the validated end-user JWT; never contains the raw token. */
public record AuthenticatedUser(
        String login,
        UUID publicId,
        String firstName,
        String lastName,
        List<String> authorities) {

    public AuthenticatedUser {
        authorities = List.copyOf(authorities);
    }
}
