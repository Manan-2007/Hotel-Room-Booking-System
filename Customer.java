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
}
