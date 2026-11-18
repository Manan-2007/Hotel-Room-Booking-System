package hotel.model;

/**
 * The four kinds of room the hotel has.
 * An enum instead of "Single"/"Double" strings: a typo becomes a compile error,
 * and each type's nightly rate lives in one place.
 */
public enum RoomType {
    SINGLE("Single", 1500),
    DOUBLE("Double", 2500),
    DELUXE("Deluxe", 4000),
    SUITE("Suite", 7500);

    private final String label;
    private final double pricePerNight;

    RoomType(String label, double pricePerNight) {
        this.label = label;
        this.pricePerNight = pricePerNight;
    }

    public String getLabel() { return label; }
    public double getPricePerNight() { return pricePerNight; }

    /** Shown in menus, cards and receipts, e.g. "Deluxe". */
    @Override
    public String toString() { return label; }
}
