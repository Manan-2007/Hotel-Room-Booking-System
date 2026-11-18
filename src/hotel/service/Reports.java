package hotel.service;

import hotel.model.Booking;
import hotel.model.Room;
import hotel.model.RoomType;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Summary figures for the Reports page and console menu.
 * Written with streams: each method reads as "take the bookings, keep these, add up / group by that".
 */
public class Reports {
    private final Hotel hotel;

    public Reports(Hotel hotel) { this.hotel = hotel; }

    /** Bookings that count towards revenue (everything except cancellations). */
    private Stream<Booking> kept() {
        return hotel.getBookings().stream().filter(b -> !b.isCancelled());
    }

    public long totalBookings() { return hotel.getBookings().size(); }

    public long keptBookings() { return kept().count(); }

    public long cancelledBookings() {
        return hotel.getBookings().stream().filter(Booking::isCancelled).count();
    }

    /** Share of all bookings that were cancelled, 0 to 1. */
    public double cancellationRate() {
        long total = totalBookings();
        return total == 0 ? 0 : (double) cancelledBookings() / total;
    }

    /** Total charged (incl. GST) for every booking that wasn't cancelled. */
    public double totalRevenue() {
        return kept().mapToDouble(Booking::getTotal).sum();
    }

    public double averageNights() {
        return kept().mapToLong(Booking::getNights).average().orElse(0);
    }

    /** Rooms with a guest tonight (the night from today to tomorrow). */
    public long roomsOccupiedTonight() {
        LocalDate today = LocalDate.now();
        return kept().filter(b -> b.overlaps(today, today.plusDays(1)))
                .map(Booking::getRoom).distinct().count();
    }

    public long totalRooms() { return hotel.getAllRooms().size(); }

    // EnumMap keeps the types in enum order: Single, Double, Deluxe, Suite
    public Map<RoomType, Long> roomsByType() {
        return hotel.getAllRooms().stream()
                .collect(Collectors.groupingBy(Room::getType, () -> new EnumMap<>(RoomType.class), Collectors.counting()));
    }

    public Map<RoomType, Long> bookingsByType() {
        return kept().collect(Collectors.groupingBy(b -> b.getRoom().getType(),
                () -> new EnumMap<>(RoomType.class), Collectors.counting()));
    }

    public Map<RoomType, Double> revenueByType() {
        return kept().collect(Collectors.groupingBy(b -> b.getRoom().getType(),
                () -> new EnumMap<>(RoomType.class), Collectors.summingDouble(Booking::getTotal)));
    }

    /** Guests arriving from today up to (not including) today + days, earliest first. */
    public List<Booking> upcomingCheckIns(int days) {
        LocalDate today = LocalDate.now(), end = today.plusDays(days);
        return kept().filter(b -> !b.getCheckIn().isBefore(today) && b.getCheckIn().isBefore(end))
                .sorted(Hotel.BY_CHECK_IN)
                .collect(Collectors.toList());
    }
}
