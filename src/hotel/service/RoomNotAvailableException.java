package hotel.service;

/** The request is valid, but another booking already holds the room for one of those nights. */
public class RoomNotAvailableException extends BookingException {
    private final int roomNumber;

    public RoomNotAvailableException(int roomNumber) {
        super("Room " + roomNumber + " is already booked for some of those nights.");
        this.roomNumber = roomNumber;
    }

    public int getRoomNumber() { return roomNumber; }
}
