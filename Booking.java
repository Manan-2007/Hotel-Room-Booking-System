import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** One reservation: a customer, a room and a stay period. */
public class Booking {
    /** GST slab limit: rooms up to this nightly rate are taxed at the lower rate. */
    public static final double GST_SLAB_LIMIT = 7500;
    public static final double GST_LOW = 0.05;
    public static final double GST_HIGH = 0.18;

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

    /** GST rate for a given nightly room rate (5% up to Rs. 7,500, 18% above). */
    public static double taxRateFor(double pricePerNight) {
        return pricePerNight <= GST_SLAB_LIMIT ? GST_LOW : GST_HIGH;
    }

    /** Label such as "GST (5%)". */
    public static String taxLabelFor(double pricePerNight) {
        return String.format("GST (%.0f%%)", taxRateFor(pricePerNight) * 100);
    }

    public long getNights() { return ChronoUnit.DAYS.between(checkIn, checkOut); }
    public double getSubtotal() { return getNights() * room.getPricePerNight(); }
    public double getTaxRate() { return taxRateFor(room.getPricePerNight()); }
    public double getTax() { return getSubtotal() * getTaxRate(); }
    public double getTotal() { return getSubtotal() + getTax(); }

    /** True if this active booking occupies the room during any night in [in, out). */
    public boolean overlaps(LocalDate in, LocalDate out) {
        return !cancelled && in.isBefore(checkOut) && checkIn.isBefore(out);
    }

    /** Only bookings whose stay has not started yet can be cancelled. */
    public boolean canCancel() {
        return !cancelled && !checkIn.isBefore(LocalDate.now());
    }

    /** True for bookings that still hold the room now or in the future. */
    public boolean isActive() {
        return !cancelled && !checkOut.isBefore(LocalDate.now());
    }

    /** Cancelled, Completed (checked out), Ongoing (guest staying now) or Upcoming. */
    public String getStatus() {
        LocalDate today = LocalDate.now();
        if (cancelled) return "Cancelled";
        if (checkOut.isBefore(today)) return "Completed";
        if (!checkIn.isAfter(today)) return "Ongoing";
        return "Upcoming";
    }

    public void print() {
        System.out.println("-------------------------------------------");
        System.out.println(" Booking ID : " + id + "  [" + getStatus().toUpperCase() + "]");
        System.out.println(" Customer   : " + customer.getName());
        System.out.println(" Phone      : " + customer.getPhone());
        System.out.println(" Email      : " + customer.getEmail());
        System.out.println(" ID Proof   : " + customer.getMaskedIdProof());
        System.out.println(" Room       : " + room.getNumber() + " (" + room.getType() + ")");
        System.out.println(" Check-in   : " + checkIn);
        System.out.println(" Check-out  : " + checkOut + "  (" + getNights() + " night(s))");
        System.out.printf (" Rate       : Rs. %,.2f x %d = Rs. %,.2f%n", room.getPricePerNight(), getNights(), getSubtotal());
        System.out.printf (" %-10s : Rs. %,.2f%n", taxLabelFor(room.getPricePerNight()), getTax());
        System.out.printf (" TOTAL      : Rs. %,.2f%n", getTotal());
        System.out.println("-------------------------------------------");
    }
}
