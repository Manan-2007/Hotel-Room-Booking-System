package hotel.service;

import hotel.dao.BookingDao;
import hotel.dao.FileBookingDao;
import hotel.model.Booking;
import hotel.model.Customer;
import hotel.model.Room;
import hotel.model.RoomType;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The booking rules: which rooms exist, whether a room is free, and booking / cancelling.
 * Changes are kept in memory; the screen that made them calls save() afterwards.
 */
public class Hotel {
    /** Earliest check-in first; same day goes by booking ID. Used by the console list and the reports. */
    public static final Comparator<Booking> BY_CHECK_IN =
            Comparator.comparing(Booking::getCheckIn).thenComparing(Booking::getId);

    private final Repository<Room> rooms = new Repository<>();
    private final Repository<Booking> bookings = new Repository<>();
    private final BookingDao dao;
    private int nextId = 1001;

    public Hotel(String dataFileName) {
        this(new FileBookingDao(dataFileName));
    }

    public Hotel(BookingDao dao) {
        this.dao = dao;
        // 10 rooms across 4 types
        for (int n = 101; n <= 103; n++) rooms.add(new Room(n, RoomType.SINGLE));
        for (int n = 201; n <= 204; n++) rooms.add(new Room(n, RoomType.DOUBLE));
        for (int n = 301; n <= 302; n++) rooms.add(new Room(n, RoomType.DELUXE));
        rooms.add(new Room(401, RoomType.SUITE));

        for (Booking b : dao.loadAll(this::findRoom)) {
            if (!bookings.add(b)) {
                System.out.println("Warning: skipped duplicate booking ID " + b.getId());
                continue;
            }
            nextId = Math.max(nextId, b.getId() + 1);
        }
    }

    public List<Room> getAllRooms() { return rooms.getAll(); }
    public List<Booking> getBookings() { return bookings.getAll(); }

    public Room findRoom(int number) { return rooms.findById(number); }
    public Booking findBooking(int id) { return bookings.findById(id); }

    public boolean isAvailable(Room room, LocalDate in, LocalDate out) {
        for (Booking b : bookings.getAll())
            if (b.getRoom() == room && b.overlaps(in, out)) return false;
        return true;
    }

    public List<Room> availableRooms(LocalDate in, LocalDate out) {
        List<Room> list = new ArrayList<>();
        for (Room r : rooms.getAll()) if (isAvailable(r, in, out)) list.add(r);
        return list;
    }

    /**
     * Creates a booking for the given nights (check-out day is not charged).
     * @throws InvalidBookingException if details are missing or the dates are wrong
     * @throws RoomNotAvailableException if the room is already booked for any of those nights
     */
    public synchronized Booking book(Customer c, Room room, LocalDate in, LocalDate out)
            throws InvalidBookingException, RoomNotAvailableException {
        if (c == null || room == null || in == null || out == null)
            throw new InvalidBookingException("Customer, room and dates are required.");
        if (in.isBefore(LocalDate.now()))
            throw new InvalidBookingException("Check-in cannot be in the past.");
        if (!out.isAfter(in))
            throw new InvalidBookingException("Check-out must be after check-in.");
        if (!isAvailable(room, in, out))
            throw new RoomNotAvailableException(room.getNumber());
        Booking b = new Booking(nextId++, c, room, in, out);
        bookings.add(b);
        return b;
    }

    /** Cancels an upcoming booking. Returns false if it does not exist, is already cancelled, or has started. */
    public synchronized boolean cancel(int bookingId) {
        Booking b = findBooking(bookingId);
        if (b == null || !b.canCancel()) return false;
        b.cancel();
        return true;
    }

    /**
     * Writes all bookings to storage.
     * synchronized: the GUI saves on a background thread, and this stops a booking
     * or cancellation from changing the list halfway through a save.
     */
    public synchronized void save() throws IOException {
        dao.saveAll(bookings.getAll());
    }
}
