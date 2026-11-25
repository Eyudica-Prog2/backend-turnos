package ar.edu.um.turnos.appointment.reservation.application.exception;

public class ReservationException extends RuntimeException {

    private final String code;
    private final int httpStatus;

    public ReservationException(String code, int httpStatus, String message) {
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
