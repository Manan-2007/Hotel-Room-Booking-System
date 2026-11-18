package hotel.model;

/** A hotel room. Its type decides the nightly rate. */
public class Room implements Identifiable {
    private final int number;
    private final RoomType type;

    public Room(int number, RoomType type) {
        this.number = number;
        this.type = type;
    }

    public int getNumber() { return number; }
    public RoomType getType() { return type; }
    public double getPricePerNight() { return type.getPricePerNight(); }

    /** A room is identified by its room number. */
    @Override
    public int getId() { return number; }

    @Override
    public String toString() {
        return String.format("Room %-4d %-8s Rs. %,.2f / night", number, type, getPricePerNight());
    }
}
