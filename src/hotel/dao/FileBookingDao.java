package hotel.dao;

import hotel.model.Booking;
import hotel.model.Customer;
import hotel.model.Room;

import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Saves bookings to a pipe-separated text file, one booking per line. */
public class FileBookingDao implements BookingDao {
    private final File file;

    public FileBookingDao(String fileName) {
        this.file = new File(fileName);
    }

    @Override
    public List<Booking> loadAll(Function<Integer, Room> findRoom) {
        List<Booking> result = new ArrayList<>();
        if (!file.exists()) return result;
        int lineNo = 0, skipped = 0;
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) continue;
                // each line is parsed on its own so one bad line doesn't lose the rest
                try {
                    result.add(parse(line, findRoom));
                } catch (RuntimeException e) {
                    skipped++;
                    System.out.println("Warning: skipped line " + lineNo + " of " + file.getName() + " (" + e.getMessage() + ")");
                }
            }
        } catch (IOException e) {
            System.out.println("Warning: could not read saved data (" + e.getMessage() + ")");
        }
        if (skipped > 0) System.out.println("Loaded " + result.size() + " booking(s), skipped " + skipped + " bad line(s).");
        return result;
    }

    private Booking parse(String line, Function<Integer, Room> findRoom) {
        String[] p = line.split("\\|", -1);
        if (p.length < 9) throw new IllegalArgumentException("expected 9 fields, found " + p.length);
        Room room = findRoom.apply(Integer.parseInt(p[5].trim()));
        if (room == null) throw new IllegalArgumentException("unknown room " + p[5]);
        Booking b = new Booking(Integer.parseInt(p[0].trim()), new Customer(p[1], p[2], p[3], p[4]),
                room, LocalDate.parse(p[6].trim()), LocalDate.parse(p[7].trim()));
        if (Boolean.parseBoolean(p[8].trim())) b.cancel();
        return b;
    }

    @Override
    public void saveAll(List<Booking> bookings) throws IOException {
        // BufferedWriter (not PrintWriter) so a failed write throws instead of failing silently
        try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
            for (Booking b : bookings) {
                Customer c = b.getCustomer();
                w.write(String.join("|", String.valueOf(b.getId()), clean(c.getName()), clean(c.getPhone()),
                        clean(c.getEmail()), clean(c.getIdProof()), String.valueOf(b.getRoom().getNumber()),
                        b.getCheckIn().toString(), b.getCheckOut().toString(), String.valueOf(b.isCancelled())));
                w.newLine();
            }
        }
    }

    private static String clean(String s) { return s.replace("|", "/"); }
}
