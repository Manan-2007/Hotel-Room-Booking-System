package hotel.service;

/** The request itself is wrong: missing details, a past check-in, or check-out not after check-in. */
public class InvalidBookingException extends BookingException {
    public InvalidBookingException(String message) { super(message); }
}
