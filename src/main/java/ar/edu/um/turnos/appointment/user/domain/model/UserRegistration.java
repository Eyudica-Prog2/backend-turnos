package ar.edu.um.turnos.appointment.user.domain.model;

import java.util.regex.Pattern;
import lombok.Builder;
import lombok.Getter;

/**
 * Data of a registration, as it travels from the edge of the application to the use case.
 *
 * <p>The rules that define a valid registration live here (section 3.2 of the statement and
 * section 13 of the reference): they are checked by {@link #validate()}, which the use case
 * always calls, so an invalid registration is refused no matter which entry point (HTTP,
 * test or future message) produced it. The HTTP layer repeats the same rules as Jakarta
 * constraints to answer a precise {@code fieldErrors} list, but the DTO is never the only
 * place where the rules exist.</p>
 *
 * <p>The password is held in clear text for the few microseconds between the request and the
 * hashing: it is never logged, never persisted and never returned.</p>
 */
@Getter
@Builder
public class UserRegistration {

    /** Login: 3 to 50 characters, letters, digits, dot, underscore or hyphen. */
    public static final String LOGIN_PATTERN = "^[a-zA-Z0-9._-]{3,50}$";

    private static final Pattern LOGIN = Pattern.compile(LOGIN_PATTERN);

    private static final int MIN_PASSWORD_LENGTH = 4;
    private static final int MAX_PASSWORD_LENGTH = 100;
    private static final int MAX_NAME_LENGTH = 50;
    private static final int MAX_EMAIL_LENGTH = 254;
    private static final int MAX_IMAGE_URL_LENGTH = 254;
    private static final int MIN_LANG_KEY_LENGTH = 2;
    private static final int MAX_LANG_KEY_LENGTH = 10;

    private final String login;
    private final String password;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String imageUrl;
    private final String langKey;

    /**
     * @return the e-mail in lower case: the form used for storage and for the uniqueness
     *         rule, because e-mail addresses are compared without case
     */
    public String getNormalizedEmail() {
        return email == null ? null : email.toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * Checks every rule of a valid registration.
     *
     * <p>Only the first violated rule is reported: the message is self contained, so the
     * caller can answer it without knowing where the rule is implemented.</p>
     *
     * @throws IllegalArgumentException with a self contained message when the data is not
     *                                  a valid registration
     */
    public void validate() {
        requirePresent(login, "login");
        if (!LOGIN.matcher(login).matches()) {
            throw new IllegalArgumentException(
                    "The login must be 3 to 50 characters made of letters, digits, '.', '_' or '-'.");
        }
        requirePresent(password, "password");
        if (password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "The password must be " + MIN_PASSWORD_LENGTH + " to " + MAX_PASSWORD_LENGTH
                            + " characters long.");
        }
        requirePresent(firstName, "firstName");
        requireLength(firstName, MAX_NAME_LENGTH, "firstName");
        requirePresent(lastName, "lastName");
        requireLength(lastName, MAX_NAME_LENGTH, "lastName");
        requirePresent(email, "email");
        if (email.length() > MAX_EMAIL_LENGTH || !email.contains("@")) {
            throw new IllegalArgumentException("The email must be a valid address of at most "
                    + MAX_EMAIL_LENGTH + " characters.");
        }
        requirePresent(langKey, "langKey");
        if (langKey.length() < MIN_LANG_KEY_LENGTH || langKey.length() > MAX_LANG_KEY_LENGTH) {
            throw new IllegalArgumentException(
                    "The langKey must be " + MIN_LANG_KEY_LENGTH + " to " + MAX_LANG_KEY_LENGTH
                            + " characters long.");
        }
        if (imageUrl != null) {
            requireLength(imageUrl, MAX_IMAGE_URL_LENGTH, "imageUrl");
        }
    }

    private static void requirePresent(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("The " + field + " is required.");
        }
    }

    private static void requireLength(String value, int max, String field) {
        if (value.length() > max) {
            throw new IllegalArgumentException("The " + field + " must be at most " + max + " characters long.");
        }
    }
}
