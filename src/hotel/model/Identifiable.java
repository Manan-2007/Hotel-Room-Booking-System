package hotel.model;

/** Anything with a unique number, so one Repository class can store rooms and bookings alike. */
public interface Identifiable {
    int getId();
}
