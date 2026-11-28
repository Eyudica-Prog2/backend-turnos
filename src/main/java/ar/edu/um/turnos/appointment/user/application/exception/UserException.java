package ar.edu.um.turnos.appointment.user.application.exception;

/**
 * Single application exception of the {@code user} slice.
 *
 * <p>Every instance carries a stable functional {@code code}, the HTTP status that must be
 * exposed and a self contained {@code detail}, so the shared error handler can translate it
 * without rebuilding a message at the catching point (contract v1, section 13: clients
 * branch on {@code status} + {@code code}, never on the text of {@code detail}).</p>
 *
 * <p>No instance ever contains a password, a hash or a token.</p>
 */
public class UserException extends RuntimeException {

    private final String code;
    private final int httpStatus;

    private UserException(String code, int httpStatus, String detail) {
        super(detail);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    /**
     * The login of the registration is already used by another account.
     *
     * <p>Answered with {@code 400} (not {@code 409}), following the error matrix of the
     * contract: {@code registro | 400 | USERNAME_ALREADY_EXISTS}.</p>
     */
    public static UserException usernameAlreadyExists() {
        return new UserException("USERNAME_ALREADY_EXISTS", 400,
                "That login is already registered by another account.");
    }

    /**
     * The e-mail of the registration is already used by another account.
     *
     * <p>Answered with {@code 400}, following the error matrix of the contract:
     * {@code registro | 400 | EMAIL_ALREADY_EXISTS}.</p>
     */
    public static UserException emailAlreadyExists() {
        return new UserException("EMAIL_ALREADY_EXISTS", 400,
                "That email is already registered by another account.");
    }

    /**
     * The registration does not describe a valid user, or the body of an authentication is
     * empty.
     *
     * @param detail self contained description produced by the rule that rejected it
     */
    public static UserException invalidInput(String detail) {
        return new UserException("VALIDATION_ERROR", 400, detail);
    }

    /**
     * The credentials do not identify any active account.
     *
     * <p>The same answer is produced for an unknown login and for a wrong password, so the
     * endpoint cannot be used to discover which accounts exist.</p>
     */
    public static UserException invalidCredentials() {
        return new UserException("UNAUTHORIZED", 401,
                "Invalid username or password.");
    }

    /**
     * The identity carried by a valid token does not resolve to an account anymore: the
     * session must be renewed.
     */
    public static UserException unknownIdentity() {
        return new UserException("UNAUTHORIZED", 401,
                "The authenticated identity does not match any account; please sign in again.");
    }
}
