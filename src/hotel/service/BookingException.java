package hotel.service;

/**
 * Parent of every "this booking can't be made" error.
 * It is a checked exception, so the compiler makes every caller of Hotel.book() handle it.
 */
public class BookingException extends Exception {
    public BookingException(String message) { super(message); }
}
