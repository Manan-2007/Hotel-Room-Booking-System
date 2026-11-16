import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.function.Consumer;

/** Styled Swing front-end for the Hotel Room Booking System. */
public class HotelGUI {
    // ---------- theme ----------
    static final Color NAVY = new Color(0x14213D), NAVY2 = new Color(0x1F3A68), GOLD = new Color(0xFCA311);
    static final Color BG = new Color(0xF4F6FA), TEXT = new Color(0x1B2433), MUTED = new Color(0x6B7686);
    static final Color GREEN = new Color(0x2E9E6B), RED = new Color(0xD64550), BORDER = new Color(0xD5DBE5);
    static final Font F = new Font("SansSerif", Font.PLAIN, 13);
    static final Font FB = new Font("SansSerif", Font.BOLD, 13);

    private final Hotel hotel = new Hotel("bookings.txt");
    private JFrame frame;
    private Room selectedRoom;

    private final JPanel root = new JPanel(new BorderLayout());
    private final CardLayout pages = new CardLayout();
    private final JPanel pageHolder = new JPanel(pages);
    private NavButton navBook, navBookings;

    private final JTextField inField = field(LocalDate.now().plusDays(1).toString());
    private final JSpinner nightsSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 30, 1));
    private final JLabel outLabel = new JLabel();
    private final JLabel statusLabel = new JLabel();
    private final JPanel roomsGrid = new JPanel(new GridLayout(0, 3, 14, 14));

    private final JTextField nameField = field(""), phoneField = field(""), emailField = field(""), idField = field("");
    private final JLabel sumRoom = new JLabel("-"), sumStay = new JLabel("-"), sumSub = new JLabel("-"),
            sumTax = new JLabel("-"), sumTotal = new JLabel("Rs. 0.00");

    private final JLabel chipAvail = new JLabel("0"), chipActive = new JLabel("0");
    private final DefaultTableModel bookingModel = new DefaultTableModel(
            new String[]{"ID", "Customer", "Room", "Check-in", "Check-out", "Total (Rs.)", "Status"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable bookingTable = new JTable(bookingModel);

    public HotelGUI() {
        root.setBackground(BG);
        root.add(buildHeader(), BorderLayout.NORTH);
        pageHolder.setOpaque(false);
        pageHolder.add(buildBookPage(), "book");
        pageHolder.add(buildBookingsPage(), "bookings");
        root.add(pageHolder, BorderLayout.CENTER);

        DocumentListener dl = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refreshRooms(); }
            public void removeUpdate(DocumentEvent e) { refreshRooms(); }
            public void changedUpdate(DocumentEvent e) { refreshRooms(); }
        };
        inField.getDocument().addDocumentListener(dl);
        nightsSpinner.addChangeListener(e -> refreshRooms());
        refreshRooms();
        refreshBookings();
    }

    public JComponent getContent() { return root; }

    public void show() {
        frame = new JFrame("Grand Stay Hotel - Room Booking System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(root);
        frame.setSize(1040, 760);
        frame.setMinimumSize(new Dimension(980, 700));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // ---------- header ----------
    private JComponent buildHeader() {
        JPanel h = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, NAVY, getWidth(), 0, NAVY2));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(GOLD);
                g2.fillRect(0, getHeight() - 3, getWidth(), 3);
                g2.dispose();
            }
        };
        h.setBorder(new EmptyBorder(14, 24, 16, 24));

        JLabel title = new JLabel("GRAND STAY HOTEL");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(Color.WHITE);
        JLabel sub = new JLabel("Room Booking System");
        sub.setFont(F);
        sub.setForeground(GOLD);
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);
        left.add(title);
        left.add(sub);

        navBook = new NavButton("Book a Room");
        navBookings = new NavButton("Bookings");
        ButtonGroup bg = new ButtonGroup();
        bg.add(navBook);
        bg.add(navBookings);
        navBook.addActionListener(e -> showPage("book"));
        navBookings.addActionListener(e -> showPage("bookings"));
        navBook.setSelected(true);

        JPanel right = new JPanel(new GridBagLayout());
        right.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 6, 0, 6);
        right.add(chip(chipAvail, "Rooms free"), c);
        right.add(chip(chipActive, "Active bookings"), c);
        c.insets = new Insets(0, 24, 0, 0);
        right.add(navBook, c);
        c.insets = new Insets(0, 4, 0, 0);
        right.add(navBookings, c);

        h.add(left, BorderLayout.WEST);
        h.add(right, BorderLayout.EAST);
        return h;
    }

    private JComponent chip(JLabel value, String caption) {
        RoundedPanel p = new RoundedPanel(new Color(255, 255, 255, 32), null, 16);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(6, 16, 6, 16));
        value.setFont(new Font("SansSerif", Font.BOLD, 20));
        value.setForeground(GOLD);
        value.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel cap = new JLabel(caption);
        cap.setFont(new Font("SansSerif", Font.PLAIN, 11));
        cap.setForeground(Color.WHITE);
        cap.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(value);
        p.add(cap);
        return p;
    }

    void showPage(String name) {
        pages.show(pageHolder, name);
        (name.equals("book") ? navBook : navBookings).setSelected(true);
        if (name.equals("bookings")) refreshBookings();
    }

    // ---------- booking page ----------
    private JComponent buildBookPage() {
        JPanel page = new JPanel(new BorderLayout(16, 14));
        page.setOpaque(false);
        page.setBorder(new EmptyBorder(16, 24, 18, 24));

        // search bar (two rows so nothing wraps/clips)
        RoundedPanel bar = new RoundedPanel(Color.WHITE, BORDER, 16);
        bar.setLayout(new GridLayout(2, 1));
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        row1.setOpaque(false);
        row1.add(caption("Check-in (YYYY-MM-DD)"));
        inField.setColumns(10);
        row1.add(inField);
        row1.add(caption("Nights"));
        nightsSpinner.setFont(F);
        row1.add(nightsSpinner);
        outLabel.setFont(FB);
        outLabel.setForeground(TEXT);
        row1.add(outLabel);
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        row2.setOpaque(false);
        statusLabel.setFont(FB);
        row2.add(statusLabel);
        bar.add(row1);
        bar.add(row2);
        bar.setBorder(new EmptyBorder(2, 6, 8, 6));
        page.add(bar, BorderLayout.NORTH);

        // room cards
        roomsGrid.setOpaque(false);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(roomsGrid, BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(wrap);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        page.add(sp, BorderLayout.CENTER);

        page.add(buildForm(), BorderLayout.EAST);
        return page;
    }

    private JComponent buildForm() {
        RoundedPanel f = new RoundedPanel(Color.WHITE, BORDER, 18);
        f.setLayout(new BoxLayout(f, BoxLayout.Y_AXIS));
        f.setBorder(new EmptyBorder(16, 18, 16, 18));
        f.setPreferredSize(new Dimension(300, 10));

        JLabel t = new JLabel("Guest Details");
        t.setFont(new Font("SansSerif", Font.BOLD, 17));
        t.setForeground(TEXT);
        left(t);
        f.add(t);
        f.add(Box.createVerticalStrut(8));
        addField(f, "Full name", nameField);
        addField(f, "Phone (10 digits)", phoneField);
        addField(f, "Email", emailField);
        addField(f, "ID proof (Aadhaar / Passport no.)", idField);

        f.add(Box.createVerticalStrut(6));
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
        f.add(sep);
        f.add(Box.createVerticalStrut(8));
        f.add(summaryRow("Room", sumRoom, false));
        f.add(summaryRow("Stay", sumStay, false));
        f.add(summaryRow("Subtotal", sumSub, false));
        f.add(summaryRow("GST (12%)", sumTax, false));
        f.add(Box.createVerticalStrut(4));
        f.add(summaryRow("TOTAL", sumTotal, true));
        f.add(Box.createVerticalGlue());
        f.add(Box.createVerticalStrut(10));

        RoundButton book = new RoundButton("Book Selected Room", GOLD, NAVY);
        book.addActionListener(e -> book());
        book.setAlignmentX(Component.LEFT_ALIGNMENT);
        book.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        f.add(book);
        return f;
    }

    private void addField(JPanel p, String cap, JTextField tf) {
        JLabel l = caption(cap);
        left(l);
        left(tf);
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        p.add(l);
        p.add(Box.createVerticalStrut(3));
        p.add(tf);
        p.add(Box.createVerticalStrut(8));
    }

    private JPanel summaryRow(String key, JLabel value, boolean big) {
        JPanel r = new JPanel(new BorderLayout());
        r.setOpaque(false);
        JLabel k = new JLabel(key);
        k.setFont(big ? new Font("SansSerif", Font.BOLD, 15) : F);
        k.setForeground(big ? TEXT : MUTED);
        value.setFont(big ? new Font("SansSerif", Font.BOLD, 20) : FB);
        value.setForeground(big ? new Color(0xC77700) : TEXT);
        r.add(k, BorderLayout.WEST);
        r.add(value, BorderLayout.EAST);
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, big ? 32 : 24));
        left(r);
        return r;
    }

    // ---------- bookings page ----------
    private JComponent buildBookingsPage() {
        JPanel page = new JPanel(new BorderLayout(0, 12));
        page.setOpaque(false);
        page.setBorder(new EmptyBorder(16, 24, 18, 24));

        JLabel t = new JLabel("All Bookings");
        t.setFont(new Font("SansSerif", Font.BOLD, 20));
        t.setForeground(TEXT);
        page.add(t, BorderLayout.NORTH);

        bookingTable.setRowHeight(36);
        bookingTable.setShowGrid(false);
        bookingTable.setIntercellSpacing(new Dimension(0, 0));
        bookingTable.setFont(F);
        bookingTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bookingTable.setSelectionBackground(new Color(0xFFE9C2));
        bookingTable.setSelectionForeground(TEXT);
        bookingTable.setDefaultRenderer(Object.class, new RowRenderer());
        bookingTable.getTableHeader().setDefaultRenderer(new HeaderRenderer());
        bookingTable.getTableHeader().setReorderingAllowed(false);
        bookingTable.getColumnModel().getColumn(0).setMaxWidth(80);
        bookingTable.getColumnModel().getColumn(2).setMaxWidth(80);
        JScrollPane sp = new JScrollPane(bookingTable);
        sp.setBorder(new LineBorder(BORDER, 1, true));
        sp.getViewport().setBackground(Color.WHITE);
        page.add(sp, BorderLayout.CENTER);

        RoundButton bill = new RoundButton("View Bill", NAVY, Color.WHITE);
        bill.addActionListener(e -> { Booking b = selectedBooking(); if (b != null) showBill(b); });
        RoundButton cancel = new RoundButton("Cancel Selected Booking", RED, Color.WHITE);
        cancel.addActionListener(e -> cancelSelected());
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btns.setOpaque(false);
        btns.add(bill);
        btns.add(cancel);
        page.add(btns, BorderLayout.SOUTH);
        return page;
    }

    // ---------- logic ----------
    private LocalDate[] dates() {
        try {
            LocalDate in = LocalDate.parse(inField.getText().trim());
            if (in.isBefore(LocalDate.now())) return null;
            return new LocalDate[]{in, in.plusDays((Integer) nightsSpinner.getValue())};
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private void refreshRooms() {
        LocalDate[] d = dates();
        if (d == null) {
            outLabel.setText("");
            statusLabel.setText("Enter a valid check-in date (YYYY-MM-DD, today or later)");
            statusLabel.setForeground(RED);
            return;
        }
        outLabel.setText("Check-out: " + d[1]);
        roomsGrid.removeAll();
        int free = 0, total = hotel.getAllRooms().size();
        boolean selectionStillFree = false;
        for (Room r : hotel.getAllRooms()) {
            boolean ok = hotel.isAvailable(r, d[0], d[1]);
            if (ok) free++;
            boolean sel = ok && selectedRoom != null && selectedRoom.getNumber() == r.getNumber();
            if (sel) selectionStillFree = true;
            roomsGrid.add(new RoomCard(r, ok, sel, this::selectRoom));
        }
        if (!selectionStillFree) selectedRoom = null;
        statusLabel.setText(free + " of " + total + " rooms available for these dates");
        statusLabel.setForeground(free > 0 ? GREEN : RED);
        chipAvail.setText(String.valueOf(free));
        roomsGrid.revalidate();
        roomsGrid.repaint();
        updateSummary();
    }

    void selectRoom(Room r) {
        selectedRoom = r;
        for (Component c : roomsGrid.getComponents())
            if (c instanceof RoomCard) {
                RoomCard rc = (RoomCard) c;
                rc.selected = rc.available && rc.room.getNumber() == r.getNumber();
                rc.repaint();
            }
        updateSummary();
    }

    private void updateSummary() {
        LocalDate[] d = dates();
        if (selectedRoom == null || d == null) {
            sumRoom.setText("none selected");
            sumStay.setText("-");
            sumSub.setText("-");
            sumTax.setText("-");
            sumTotal.setText("Rs. 0.00");
            return;
        }
        long n = d[1].toEpochDay() - d[0].toEpochDay();
        double sub = n * selectedRoom.getPricePerNight(), tax = sub * Booking.GST_RATE;
        sumRoom.setText(selectedRoom.getNumber() + " (" + selectedRoom.getType() + ")");
        sumStay.setText(n + " night(s)");
        sumSub.setText(String.format("Rs. %,.2f", sub));
        sumTax.setText(String.format("Rs. %,.2f", tax));
        sumTotal.setText(String.format("Rs. %,.2f", sub + tax));
    }

    private void book() {
        LocalDate[] d = dates();
        if (d == null) { error("Please enter a valid check-in date (YYYY-MM-DD, today or later)."); return; }
        if (selectedRoom == null) { error("Click an available room card first."); return; }
        if (nameField.getText().isBlank() || emailField.getText().isBlank() || idField.getText().isBlank()) {
            error("Please fill in name, email and ID proof."); return;
        }
        if (!phoneField.getText().trim().matches("\\d{10}")) { error("Phone must be exactly 10 digits."); return; }

        Customer c = new Customer(nameField.getText().trim(), phoneField.getText().trim(),
                emailField.getText().trim(), idField.getText().trim());
        Booking b = hotel.book(c, hotel.findRoom(selectedRoom.getNumber()), d[0], d[1]);
        if (b == null) { error("That room is no longer available."); refreshRooms(); return; }
        nameField.setText(""); phoneField.setText(""); emailField.setText(""); idField.setText("");
        selectedRoom = null;
        refreshRooms();
        refreshBookings();
        showBill(b);
    }

    private Booking selectedBooking() {
        int row = bookingTable.getSelectedRow();
        if (row < 0) { error("Select a booking from the table first."); return null; }
        return hotel.findBooking((Integer) bookingModel.getValueAt(row, 0));
    }

    private void cancelSelected() {
        Booking b = selectedBooking();
        if (b == null) return;
        if (b.isCancelled()) { error("This booking is already cancelled."); return; }
        if (JOptionPane.showConfirmDialog(frame, "Cancel booking " + b.getId() + " for "
                + b.getCustomer().getName() + "?", "Confirm cancellation", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            hotel.cancel(b.getId());
            refreshBookings();
            refreshRooms();
        }
    }

    private void refreshBookings() {
        bookingModel.setRowCount(0);
        int active = 0;
        for (Booking b : hotel.getBookings()) {
            if (!b.isCancelled()) active++;
            bookingModel.addRow(new Object[]{b.getId(), b.getCustomer().getName(), b.getRoom().getNumber(),
                    b.getCheckIn(), b.getCheckOut(), String.format("%,.2f", b.getTotal()),
                    b.isCancelled() ? "Cancelled" : "Confirmed"});
        }
        chipActive.setText(String.valueOf(active));
    }

    // ---------- receipt ----------
    JComponent receipt(Booking b) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        JLabel banner = new JLabel((b.isCancelled() ? "BOOKING CANCELLED" : "BOOKING CONFIRMED") + "   #" + b.getId(),
                SwingConstants.CENTER);
        banner.setOpaque(true);
        banner.setBackground(b.isCancelled() ? RED : GREEN);
        banner.setForeground(Color.WHITE);
        banner.setFont(new Font("SansSerif", Font.BOLD, 15));
        banner.setBorder(new EmptyBorder(12, 12, 12, 12));

        String s = String.format(
                "Guest     : %s%nPhone     : %s%nEmail     : %s%nID proof  : %s%n%n"
                + "Room      : %d (%s)%nCheck-in  : %s%nCheck-out : %s%nNights    : %d%n%n"
                + "Rate      : Rs. %,.2f x %d%nSubtotal  : Rs. %,.2f%nGST (12%%) : Rs. %,.2f",
                b.getCustomer().getName(), b.getCustomer().getPhone(), b.getCustomer().getEmail(),
                b.getCustomer().getIdProof(), b.getRoom().getNumber(), b.getRoom().getType(),
                b.getCheckIn(), b.getCheckOut(), b.getNights(), b.getRoom().getPricePerNight(),
                b.getNights(), b.getSubtotal(), b.getTax());
        JTextArea body = new JTextArea(s);
        body.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        body.setEditable(false);
        body.setForeground(TEXT);
        body.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel total = new JLabel(String.format("TOTAL   Rs. %,.2f", b.getTotal()), SwingConstants.CENTER);
        total.setOpaque(true);
        total.setBackground(NAVY);
        total.setForeground(GOLD);
        total.setFont(new Font("SansSerif", Font.BOLD, 20));
        total.setBorder(new EmptyBorder(14, 12, 14, 12));

        p.add(banner, BorderLayout.NORTH);
        p.add(body, BorderLayout.CENTER);
        p.add(total, BorderLayout.SOUTH);
        p.setPreferredSize(new Dimension(380, 400));
        return p;
    }

    private void showBill(Booking b) {
        JOptionPane.showMessageDialog(frame, receipt(b), "Booking Receipt", JOptionPane.PLAIN_MESSAGE);
    }

    private void error(String msg) { JOptionPane.showMessageDialog(frame, msg, "Please check", JOptionPane.WARNING_MESSAGE); }

    // ---------- small helpers ----------
    static JTextField field(String text) {
        JTextField f = new JTextField(text);
        f.setFont(F);
        f.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(6, 10, 6, 10)));
        return f;
    }

    static JLabel caption(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.PLAIN, 12));
        l.setForeground(MUTED);
        return l;
    }

    static void left(JComponent c) { c.setAlignmentX(Component.LEFT_ALIGNMENT); }

    static Color typeColor(String type) {
        switch (type) {
            case "Single": return new Color(0x3DA5D9);
            case "Double": return new Color(0x5C6BC0);
            case "Deluxe": return new Color(0x9C5BB5);
            default: return new Color(0xE0A100);
        }
    }

    // ---------- custom components ----------
    static class RoundedPanel extends JPanel {
        private final Color fill, line;
        private final int arc;
        RoundedPanel(Color fill, Color line, int arc) { this.fill = fill; this.line = line; this.arc = arc; setOpaque(false); }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            if (line != null) { g2.setColor(line); g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc); }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class RoundButton extends JButton {
        private final Color base;
        RoundButton(String text, Color base, Color textColor) {
            super(text);
            this.base = base;
            setForeground(textColor);
            setFont(new Font("SansSerif", Font.BOLD, 13));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setRolloverEnabled(true);
            setBorder(new EmptyBorder(11, 20, 11, 20));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color c = getModel().isPressed() ? base.darker() : getModel().isRollover() ? base.brighter() : base;
            g2.setColor(c);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class NavButton extends JToggleButton {
        NavButton(String text) {
            super(text);
            setFont(new Font("SansSerif", Font.BOLD, 14));
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setBorder(new EmptyBorder(8, 14, 10, 14));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addItemListener(e -> setForeground(isSelected() ? GOLD : Color.WHITE));
        }
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (isSelected()) { g.setColor(GOLD); g.fillRect(14, getHeight() - 4, getWidth() - 28, 3); }
        }
    }

    static class RoomCard extends JPanel {
        final Room room;
        final boolean available;
        boolean selected;

        RoomCard(Room room, boolean available, boolean selected, Consumer<Room> onClick) {
            this.room = room;
            this.available = available;
            this.selected = selected;
            setOpaque(false);
            setPreferredSize(new Dimension(180, 124));
            if (available) {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) { onClick.accept(room); }
                });
            }
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth() - 2, h = getHeight() - 2;
            g2.setColor(available ? Color.WHITE : new Color(0xECEEF2));
            g2.fillRoundRect(0, 0, w, h, 18, 18);
            Shape old = g2.getClip();
            g2.setClip(new RoundRectangle2D.Float(0, 0, w, h, 18, 18));
            g2.setColor(available ? typeColor(room.getType()) : new Color(0xB5BBC6));
            g2.fillRect(0, 0, w, 8);
            g2.setClip(old);
            g2.setStroke(new BasicStroke(selected ? 3f : 1f));
            g2.setColor(selected ? GOLD : BORDER);
            g2.drawRoundRect(1, 1, w - 2, h - 2, 18, 18);

            g2.setColor(available ? TEXT : MUTED);
            g2.setFont(new Font("SansSerif", Font.BOLD, 28));
            g2.drawString(String.valueOf(room.getNumber()), 16, 48);
            g2.setColor(MUTED);
            g2.setFont(F);
            g2.drawString(room.getType() + " room", 16, 68);
            g2.setColor(available ? TEXT : MUTED);
            g2.setFont(FB);
            g2.drawString(String.format("Rs. %,.0f / night", room.getPricePerNight()), 16, 92);

            String pill = !available ? "BOOKED" : selected ? "SELECTED" : "AVAILABLE";
            Color pc = !available ? RED : selected ? new Color(0xC77700) : GREEN;
            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(pill), px = w - tw - 26, py = h - 28;
            g2.setColor(new Color(pc.getRed(), pc.getGreen(), pc.getBlue(), 38));
            g2.fillRoundRect(px, py, tw + 16, 18, 18, 18);
            g2.setColor(pc);
            g2.drawString(pill, px + 8, py + 13);
            g2.dispose();
        }
    }

    static class HeaderRenderer extends DefaultTableCellRenderer {
        HeaderRenderer() { setOpaque(true); }
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            super.getTableCellRendererComponent(t, v, false, false, r, c);
            setBackground(NAVY);
            setForeground(Color.WHITE);
            setFont(FB);
            setBorder(new EmptyBorder(0, 12, 0, 12));
            setPreferredSize(new Dimension(getPreferredSize().width, 38));
            return this;
        }
    }

    static class RowRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean f, int r, int c) {
            super.getTableCellRendererComponent(t, v, sel, f, r, c);
            setBorder(new EmptyBorder(0, 12, 0, 12));
            setFont(F);
            if (!sel) setBackground(r % 2 == 0 ? Color.WHITE : new Color(0xF7F9FC));
            setForeground(TEXT);
            if (c == 6) {
                setFont(FB);
                setForeground("Cancelled".equals(v) ? RED : GREEN);
            }
            return this;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new HotelGUI().show());
    }
}
