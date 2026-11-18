Grand Stay Hotel - Room Booking System
Team: Arshpreet Kaur, Manan Kochhar, Atharv Bains

Compile:  javac *.java
Run (styled GUI):  java HotelGUI
Run (console version):  java Main

Both versions share the same logic (Hotel, Booking, Room, Customer)
and the same saved data file (bookings.txt).

Required features
 1. Display available rooms   - room cards for the chosen dates (green = free, red = booked),
                                filter by room type; the grid adapts to the window size
 2. Book a room               - click a card (or Tab + Space/Enter), fill guest details, Book
 3. Store customer details    - name, phone, email, ID proof validated and saved with each booking;
                                Bookings tab shows phone and can search by name, phone or booking ID
 4. Cancel room bookings      - Bookings tab > select > Cancel (only bookings that haven't started yet)
 5. Calculate total charges   - live bill: nights x rate + GST (5% up to Rs. 7,500/night, 18% above),
                                plus a receipt popup (ID proof masked to the last 4 characters)
Extras: date-overlap check (no double booking), booking status (Upcoming / Ongoing / Completed /
Cancelled), sortable bookings table, data saved to bookings.txt (bad lines are skipped, not fatal).
