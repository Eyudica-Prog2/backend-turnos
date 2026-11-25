package ar.edu.um.turnos.appointment.shared.error;

public class CatalogClientException extends RuntimeException {

    private final String code;
    private final int httpStatus;

    public CatalogClientException(String code, int httpStatus, String message) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
