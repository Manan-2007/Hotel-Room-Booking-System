Grand Stay Hotel - Room Booking System
Team: Arshpreet Kaur, Manan Kochhar, Atharv Bains

Run these from the project folder (the one containing src and bookings.txt):
  Compile:                javac -d out -sourcepath src src/hotel/ui/*.java
  Run (styled GUI):       java -cp out hotel.ui.HotelGUI
  Run (console version):  java -cp out hotel.ui.Main

Both versions share the same logic and the same saved data file (bookings.txt).

Project layout (one job per package)
  src/hotel/model    the data: Room, RoomType, Customer, Booking, Identifiable
  src/hotel/service  the rules: Hotel (book / cancel / availability), Reports, Repository,
                     and the booking exceptions
  src/hotel/dao      saving and loading: BookingDao (interface), FileBookingDao (text file)
  src/hotel/ui       the screens: HotelGUI (Swing) and Main (console)

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
Cancelled), sortable bookings table, Reports page (revenue by room type, occupancy tonight,
cancellations, check-ins in the next 7 days), data saved to bookings.txt (bad lines are skipped).

Where each Java topic is used
  Classes, objects, encapsulation   model/*  (private final fields + getters)
  Enum                              model/RoomType       - room types and their nightly rates
  Interfaces                        model/Identifiable, dao/BookingDao
  Inheritance                       service/BookingException -> InvalidBookingException,
                                    RoomNotAvailableException
  Checked custom exceptions         service/Hotel.book() throws them; ui/Main catches each one
                                    separately, ui/HotelGUI catches the parent class once
  Generics (bounded type)           service/Repository<T extends Identifiable>
  Collections: HashMap              service/Repository   - lookup by ID without a loop
  Collections: EnumMap, List        service/Reports, everywhere
  Comparator                        service/Hotel.BY_CHECK_IN - console list and reports order
  Lambdas & method references       Customer::isValidName (ui/Main), this::findRoom (service/Hotel)
  Streams & Collectors.groupingBy   service/Reports
  File I/O, try-with-resources      dao/FileBookingDao
  Multithreading: SwingWorker       ui/HotelGUI.saveInBackground() - saving never freezes the window
  Multithreading: scheduled task    ui/HotelGUI.startDayWatcher() - statuses refresh when the date changes
  synchronized                      service/Hotel - a background save and a new booking never clash
  java.time (LocalDate)             model/Booking, everywhere dates are used
  Swing GUI, events, layouts        ui/HotelGUI
