/** Customer details stored with every booking. */
public class Customer {
    private final String name;
    private final String phone;
    private final String email;
    private final String idProof;

    public Customer(String name, String phone, String email, String idProof) {
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.idProof = idProof;
    }

    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getIdProof() { return idProof; }

    /** ID proof with all but the last 4 characters hidden, for screens and receipts. */
    public String getMaskedIdProof() {
        if (idProof.length() <= 4) return idProof;
        return "X".repeat(idProof.length() - 4) + idProof.substring(idProof.length() - 4);
    }

    // ---------- validation shared by the GUI and the console ----------
    /** Letters, spaces, dots, apostrophes and hyphens; at least 2 letters. */
    public static boolean isValidName(String s) {
        return s != null && s.trim().matches("[A-Za-z][A-Za-z .'-]*[A-Za-z.]");
    }

    public static boolean isValidPhone(String s) {
        return s != null && s.trim().matches("\\d{10}");
    }

    public static boolean isValidEmail(String s) {
        return s != null && s.trim().matches("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+");
    }

    /** 4 to 24 letters/digits, spaces or hyphens allowed inside (Aadhaar, passport, licence, etc.). */
    public static boolean isValidIdProof(String s) {
        return s != null && s.trim().matches("[A-Za-z0-9][A-Za-z0-9 -]{2,22}[A-Za-z0-9]");
    }
}
