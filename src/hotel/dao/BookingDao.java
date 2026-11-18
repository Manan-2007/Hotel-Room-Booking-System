package hotel.dao;

import hotel.model.Booking;
import hotel.model.Room;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

/**
 * How bookings are stored. Hotel only talks to this interface, so the text file
 * could later be swapped for a database class without touching the booking rules.
 */
public interface BookingDao {
    /**
     * Reads every saved booking.
     * @param findRoom turns a saved room number back into the Room object (null if unknown)
     */
    List<Booking> loadAll(Function<Integer, Room> findRoom);

    /** Replaces the saved data with these bookings. */
    void saveAll(List<Booking> bookings) throws IOException;
}
