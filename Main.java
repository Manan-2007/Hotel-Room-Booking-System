import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/** Console menu for the Hotel Room Booking System. */
public class Main {
    private static final Scanner in = new Scanner(System.in);
    private static final Hotel hotel = new Hotel("bookings.txt");

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("      HOTEL ROOM BOOKING SYSTEM");
        System.out.println("=========================================");
        while (true) {
            System.out.println("\n1. View all rooms");
            System.out.println("2. View available rooms for dates");
            System.out.println("3. Book a room");
            System.out.println("4. Cancel a booking");
            System.out.println("5. View booking details / bill");
            System.out.println("6. List all bookings");
            System.out.println("0. Exit");
            int choice = readInt("Choose an option: ");
            switch (choice) {
                case 1: viewAllRooms(); break;
                case 2: viewAvailable(); break;
                case 3: bookRoom(); break;
                case 4: cancelBooking(); break;
                case 5: viewBooking(); break;
                case 6: listBookings(); break;
                case 0: System.out.println("Thank you for using the system. Goodbye!"); return;
                default: System.out.println("Invalid option, try again.");
            }
        }
    }

    private static void viewAllRooms() {
        System.out.println("\n--- All Rooms ---");
        for (Room r : hotel.getAllRooms()) System.out.println(r);
    }

    private static void viewAvailable() {
        LocalDate[] d = readDates();
        if (d == null) return;
        List<Room> free = hotel.availableRooms(d[0], d[1]);
        System.out.println("\n--- Available rooms: " + d[0] + " to " + d[1] + " ---");
        if (free.isEmpty()) System.out.println("No rooms available for these dates.");
        for (Room r : free) System.out.println(r);
    }

    private static void bookRoom() {
        LocalDate[] d = readDates();
        if (d == null) return;
        List<Room> free = hotel.availableRooms(d[0], d[1]);
        if (free.isEmpty()) { System.out.println("Sorry, no rooms available for these dates."); return; }
        System.out.println("\n--- Available rooms ---");
        for (Room r : free) System.out.println(r);

        Room room = hotel.findRoom(readInt("Enter room number to book: "));
        if (room == null || !free.contains(room)) { System.out.println("That room is not available."); return; }

        System.out.println("\n--- Customer details ---");
        String name = readNonEmpty("Name: ");
        String phone = readPhone();
        String email = readNonEmpty("Email: ");
        String idProof = readNonEmpty("ID proof (e.g. Aadhaar / Passport no.): ");

        Booking b = hotel.book(new Customer(name, phone, email, idProof), room, d[0], d[1]);
        if (b == null) { System.out.println("Booking failed: room no longer available."); return; }
        System.out.println("\nBooking confirmed!");
        b.print();
    }

    private static void cancelBooking() {
        int id = readInt("Enter booking ID to cancel: ");
        Booking b = hotel.findBooking(id);
        if (b == null) { System.out.println("No booking found with ID " + id); return; }
        if (b.isCancelled()) { System.out.println("This booking is already cancelled."); return; }
        b.print();
        System.out.print("Cancel this booking? (y/n): ");
        if (in.nextLine().trim().equalsIgnoreCase("y")) {
            hotel.cancel(id);
            System.out.println("Booking " + id + " cancelled. Room is available again.");
        } else {
            System.out.println("Cancellation aborted.");
        }
    }

    private static void viewBooking() {
        Booking b = hotel.findBooking(readInt("Enter booking ID: "));
        if (b == null) System.out.println("Booking not found.");
        else b.print();
    }

    private static void listBookings() {
        System.out.println("\n--- All Bookings ---");
        if (hotel.getBookings().isEmpty()) { System.out.println("No bookings yet."); return; }
        System.out.printf("%-6s %-18s %-5s %-11s %-11s %-12s %s%n",
                "ID", "Customer", "Room", "Check-in", "Check-out", "Total (Rs.)", "Status");
        for (Booking b : hotel.getBookings())
            System.out.printf("%-6d %-18s %-5d %-11s %-11s %,-12.2f %s%n", b.getId(), b.getCustomer().getName(),
                    b.getRoom().getNumber(), b.getCheckIn(), b.getCheckOut(), b.getTotal(),
                    b.isCancelled() ? "Cancelled" : "Confirmed");
    }

    // ---------- input helpers ----------
    private static LocalDate[] readDates() {
        try {
            System.out.print("Check-in date (YYYY-MM-DD): ");
            LocalDate inDate = LocalDate.parse(in.nextLine().trim());
            System.out.print("Check-out date (YYYY-MM-DD): ");
            LocalDate outDate = LocalDate.parse(in.nextLine().trim());
            if (inDate.isBefore(LocalDate.now())) { System.out.println("Check-in cannot be in the past."); return null; }
            if (!outDate.isAfter(inDate)) { System.out.println("Check-out must be after check-in."); return null; }
            return new LocalDate[]{inDate, outDate};
        } catch (DateTimeParseException e) {
            System.out.println("Invalid date format. Use YYYY-MM-DD.");
            return null;
        }
    }

    private static int readInt(String prompt) {
        System.out.print(prompt);
        try { return Integer.parseInt(in.nextLine().trim()); }
        catch (NumberFormatException e) { return -1; }
    }

    private static String readNonEmpty(String prompt) {
        String s;
        do { System.out.print(prompt); s = in.nextLine().trim(); } while (s.isEmpty());
        return s;
    }

    private static String readPhone() {
        while (true) {
            System.out.print("Phone (10 digits): ");
            String p = in.nextLine().trim();
            if (p.matches("\\d{10}")) return p;
            System.out.println("Please enter exactly 10 digits.");
        }
    }
}
