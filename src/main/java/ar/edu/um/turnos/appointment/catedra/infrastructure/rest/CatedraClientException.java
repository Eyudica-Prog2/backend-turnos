package ar.edu.um.turnos.appointment.catedra.infrastructure.rest;

public class CatedraClientException extends RuntimeException {

    private final String code;
    private final int httpStatus;

    public CatedraClientException(String code, int httpStatus, String message) {
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
