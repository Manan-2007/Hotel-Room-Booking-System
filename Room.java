/** A hotel room with a fixed type and nightly rate. */
public class Room {
    private final int number;
    private final String type;
    private final double pricePerNight;

    public Room(int number, String type, double pricePerNight) {
        this.number = number;
        this.type = type;
        this.pricePerNight = pricePerNight;
    }

    public int getNumber() { return number; }
    public String getType() { return type; }
    public double getPricePerNight() { return pricePerNight; }

    @Override
    public String toString() {
        return String.format("Room %-4d %-8s Rs. %,.2f / night", number, type, pricePerNight);
    }
}
