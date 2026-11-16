import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** One reservation: a customer, a room and a stay period. */
public class Booking {
    public static final double GST_RATE = 0.12;

    private final int id;
    private final Customer customer;
    private final Room room;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private boolean cancelled;

    public Booking(int id, Customer customer, Room room, LocalDate checkIn, LocalDate checkOut) {
        this.id = id;
        this.customer = customer;
        this.room = room;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
    }

    public int getId() { return id; }
    public Customer getCustomer() { return customer; }
    public Room getRoom() { return room; }
    public LocalDate getCheckIn() { return checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public boolean isCancelled() { return cancelled; }
    public void cancel() { cancelled = true; }

    public long getNights() { return ChronoUnit.DAYS.between(checkIn, checkOut); }
    public double getSubtotal() { return getNights() * room.getPricePerNight(); }
    public double getTax() { return getSubtotal() * GST_RATE; }
    public double getTotal() { return getSubtotal() + getTax(); }

    /** True if this active booking occupies the room during any night in [in, out). */
    public boolean overlaps(LocalDate in, LocalDate out) {
        return !cancelled && in.isBefore(checkOut) && checkIn.isBefore(out);
    }

    public void print() {
        System.out.println("-------------------------------------------");
        System.out.println(" Booking ID : " + id + (cancelled ? "  [CANCELLED]" : "  [CONFIRMED]"));
        System.out.println(" Customer   : " + customer.getName());
        System.out.println(" Phone      : " + customer.getPhone());
        System.out.println(" Email      : " + customer.getEmail());
        System.out.println(" ID Proof   : " + customer.getIdProof());
        System.out.println(" Room       : " + room.getNumber() + " (" + room.getType() + ")");
        System.out.println(" Check-in   : " + checkIn);
        System.out.println(" Check-out  : " + checkOut + "  (" + getNights() + " night(s))");
        System.out.printf (" Rate       : Rs. %,.2f x %d = Rs. %,.2f%n", room.getPricePerNight(), getNights(), getSubtotal());
        System.out.printf (" GST (12%%)  : Rs. %,.2f%n", getTax());
        System.out.printf (" TOTAL      : Rs. %,.2f%n", getTotal());
        System.out.println("-------------------------------------------");
    }
}
