import java.io.*;
import java.time.LocalDate;
import java.util.*;

/** Holds rooms and bookings, enforces availability, and saves data to a text file. */
public class Hotel {
    private final List<Room> rooms = new ArrayList<>();
    private final List<Booking> bookings = new ArrayList<>();
    private final File dataFile;
    private int nextId = 1001;

    public Hotel(String dataFileName) {
        this.dataFile = new File(dataFileName);
        // 10 rooms across 4 types
        for (int i = 101; i <= 103; i++) rooms.add(new Room(i, "Single", 1500));
        for (int i = 201; i <= 204; i++) rooms.add(new Room(i, "Double", 2500));
        for (int i = 301; i <= 302; i++) rooms.add(new Room(i, "Deluxe", 4000));
        rooms.add(new Room(401, "Suite", 7500));
        load();
    }

    public List<Room> getAllRooms() { return rooms; }
    public List<Booking> getBookings() { return bookings; }

    public Room findRoom(int number) {
        for (Room r : rooms) if (r.getNumber() == number) return r;
        return null;
    }

    public Booking findBooking(int id) {
        for (Booking b : bookings) if (b.getId() == id) return b;
        return null;
    }

    public boolean isAvailable(Room room, LocalDate in, LocalDate out) {
        for (Booking b : bookings)
            if (b.getRoom() == room && b.overlaps(in, out)) return false;
        return true;
    }

    public List<Room> availableRooms(LocalDate in, LocalDate out) {
        List<Room> list = new ArrayList<>();
        for (Room r : rooms) if (isAvailable(r, in, out)) list.add(r);
        return list;
    }

    /**
     * Creates a booking, or returns null if the room is not free for those dates.
     * @throws IllegalArgumentException if the room, customer or dates are invalid
     */
    public Booking book(Customer c, Room room, LocalDate in, LocalDate out) {
        if (c == null || room == null || in == null || out == null)
            throw new IllegalArgumentException("Customer, room and dates are required.");
        if (in.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("Check-in cannot be in the past.");
        if (!out.isAfter(in))
            throw new IllegalArgumentException("Check-out must be after check-in.");
        if (!isAvailable(room, in, out)) return null;
        Booking b = new Booking(nextId++, c, room, in, out);
        bookings.add(b);
        save();
        return b;
    }

    /** Cancels an upcoming booking. Returns false if it does not exist, is already cancelled, or has started. */
    public boolean cancel(int bookingId) {
        Booking b = findBooking(bookingId);
        if (b == null || !b.canCancel()) return false;
        b.cancel();
        save();
        return true;
    }

    // ---------- persistence (pipe-separated text file) ----------
    private void save() {
        try (PrintWriter w = new PrintWriter(new FileWriter(dataFile))) {
            for (Booking b : bookings) {
                Customer c = b.getCustomer();
                w.println(String.join("|", String.valueOf(b.getId()), clean(c.getName()), clean(c.getPhone()),
                        clean(c.getEmail()), clean(c.getIdProof()), String.valueOf(b.getRoom().getNumber()),
                        b.getCheckIn().toString(), b.getCheckOut().toString(), String.valueOf(b.isCancelled())));
            }
        } catch (IOException e) {
            System.out.println("Warning: could not save data (" + e.getMessage() + ")");
        }
    }

    private void load() {
        if (!dataFile.exists()) return;
        int lineNo = 0, skipped = 0;
        try (BufferedReader r = new BufferedReader(new FileReader(dataFile))) {
            String line;
            while ((line = r.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) continue;
                // each line is parsed on its own so one bad line doesn't lose the rest
                try {
                    String[] p = line.split("\\|", -1);
                    if (p.length < 9) throw new IllegalArgumentException("expected 9 fields, found " + p.length);
                    Room room = findRoom(Integer.parseInt(p[5].trim()));
                    if (room == null) throw new IllegalArgumentException("unknown room " + p[5]);
                    Booking b = new Booking(Integer.parseInt(p[0].trim()), new Customer(p[1], p[2], p[3], p[4]),
                            room, LocalDate.parse(p[6].trim()), LocalDate.parse(p[7].trim()));
                    if (Boolean.parseBoolean(p[8].trim())) b.cancel();
                    if (findBooking(b.getId()) != null) throw new IllegalArgumentException("duplicate booking ID " + b.getId());
                    bookings.add(b);
                    nextId = Math.max(nextId, b.getId() + 1);
                } catch (RuntimeException e) {
                    skipped++;
                    System.out.println("Warning: skipped line " + lineNo + " of " + dataFile.getName() + " (" + e.getMessage() + ")");
                }
            }
        } catch (IOException e) {
            System.out.println("Warning: could not read saved data (" + e.getMessage() + ")");
        }
        if (skipped > 0) System.out.println("Loaded " + bookings.size() + " booking(s), skipped " + skipped + " bad line(s).");
    }

    private String clean(String s) { return s.replace("|", "/"); }
}
