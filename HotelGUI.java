import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/** Styled Swing front-end for the Hotel Room Booking System. */
public class HotelGUI {
    // ---------- modern cohesive theme ----------
    static final Color PRIMARY = new Color(0x2563EB);        // Modern blue primary accent
    static final Color PRIMARY_HOVER = new Color(0x1D4ED8);
    static final Color PRIMARY_LIGHT = new Color(0xEFF6FF);  // Soft selection tint
    static final Color PRIMARY_BORDER = new Color(0x93C5FD); // Border highlight

    static final Color NAVY = new Color(0x0F172A);           // Deep slate header start
    static final Color NAVY2 = new Color(0x1E293B);          // Deep slate header end
    static final Color GOLD = PRIMARY;                       // Kept for backward compatibility

    static final Color BG = new Color(0xF8FAFC);             // Soft neutral slate-50 background
    static final Color TEXT = new Color(0x0F172A);           // High-contrast slate-900 text
    static final Color MUTED = new Color(0x64748B);          // Neutral slate-500 secondary text
    static final Color BORDER = new Color(0xE2E8F0);         // Subtle slate-200 border

    static final Color GREEN = new Color(0x10B981);          // Emerald 500
    static final Color RED = new Color(0xEF4444);            // Red 500

    // ---------- font typography hierarchy ----------
    private static final String FONT_FAMILY = detectFontFamily();

    private static String detectFontFamily() {
        String[] candidates = {"Segoe UI", "SF Pro Text", "Inter", "Helvetica Neue", "Arial"};
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Set<String> available = new HashSet<>(Arrays.asList(ge.getAvailableFontFamilyNames()));
        for (String c : candidates) {
            if (available.contains(c)) return c;
        }
        return "SansSerif";
    }

    static final Font F_TITLE = new Font(FONT_FAMILY, Font.BOLD, 22);
    static final Font F_HEAD = new Font(FONT_FAMILY, Font.BOLD, 17);
    static final Font F_SUBHEAD = new Font(FONT_FAMILY, Font.BOLD, 15);
    static final Font F_NUM_LARGE = new Font(FONT_FAMILY, Font.BOLD, 26);
    static final Font F_TOTAL = new Font(FONT_FAMILY, Font.BOLD, 20);
    static final Font F_CHIP_NUM = new Font(FONT_FAMILY, Font.BOLD, 18);
    static final Font F = new Font(FONT_FAMILY, Font.PLAIN, 13);
    static final Font FB = new Font(FONT_FAMILY, Font.BOLD, 13);
    static final Font F_CAPTION = new Font(FONT_FAMILY, Font.PLAIN, 12);
    static final Font F_BADGE = new Font(FONT_FAMILY, Font.BOLD, 10);

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
    private final JPanel roomsGrid = new JPanel(new GridLayout(0, 3, 16, 16));

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
        frame.setSize(1060, 760);
        frame.setMinimumSize(new Dimension(980, 700));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // ---------- header ----------
    private JComponent buildHeader() {
        JPanel h = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, NAVY, getWidth(), 0, NAVY2));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(PRIMARY);
                g2.fillRect(0, getHeight() - 3, getWidth(), 3);
                g2.dispose();
            }
        };
        h.setBorder(new EmptyBorder(16, 28, 16, 28));

        JLabel title = new JLabel("GRAND STAY HOTEL");
        title.setFont(F_TITLE);
        title.setForeground(Color.WHITE);
        JLabel sub = new JLabel("Room Booking System");
        sub.setFont(F_CAPTION);
        sub.setForeground(new Color(0x93C5FD));
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);
        left.add(title);
        left.add(Box.createVerticalStrut(2));
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
        c.insets = new Insets(0, 6, 0, 0);
        right.add(navBookings, c);

        h.add(left, BorderLayout.WEST);
        h.add(right, BorderLayout.EAST);
        return h;
    }

    private JComponent chip(JLabel value, String caption) {
        RoundedPanel p = new RoundedPanel(new Color(255, 255, 255, 18), new Color(255, 255, 255, 35), 12);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(6, 14, 6, 14));
        value.setFont(F_CHIP_NUM);
        value.setForeground(Color.WHITE);
        value.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel cap = new JLabel(caption);
        cap.setFont(F_CAPTION);
        cap.setForeground(new Color(0xCBD5E1));
        cap.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(value);
        p.add(Box.createVerticalStrut(2));
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
        JPanel page = new JPanel(new BorderLayout(18, 16));
        page.setOpaque(false);
        page.setBorder(new EmptyBorder(18, 28, 20, 28));

        // search bar
        RoundedPanel bar = new RoundedPanel(Color.WHITE, BORDER, 14);
        bar.setLayout(new GridLayout(2, 1, 0, 4));
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 10));
        row1.setOpaque(false);
        row1.add(caption("Check-in (YYYY-MM-DD)"));
        inField.setColumns(11);
        row1.add(inField);
        row1.add(Box.createHorizontalStrut(8));
        row1.add(caption("Nights"));
        nightsSpinner.setFont(F);
        if (nightsSpinner.getEditor() instanceof JSpinner.DefaultEditor) {
            JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) nightsSpinner.getEditor();
            editor.getTextField().setFont(F);
            editor.getTextField().setForeground(TEXT);
            editor.getTextField().setBorder(new EmptyBorder(4, 6, 4, 6));
        }
        nightsSpinner.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(2, 4, 2, 4)));
        nightsSpinner.setPreferredSize(new Dimension(75, 36));
        row1.add(nightsSpinner);
        row1.add(Box.createHorizontalStrut(10));
        outLabel.setFont(FB);
        outLabel.setForeground(TEXT);
        row1.add(outLabel);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 4));
        row2.setOpaque(false);
        statusLabel.setFont(FB);
        row2.add(statusLabel);
        bar.add(row1);
        bar.add(row2);
        bar.setBorder(new EmptyBorder(6, 10, 10, 10));
        page.add(bar, BorderLayout.NORTH);

        // room cards
        roomsGrid.setOpaque(false);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(new EmptyBorder(2, 2, 8, 8));
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
        RoundedPanel f = new RoundedPanel(Color.WHITE, BORDER, 16);
        f.setLayout(new BoxLayout(f, BoxLayout.Y_AXIS));
        f.setBorder(new EmptyBorder(20, 20, 20, 20));
        f.setPreferredSize(new Dimension(310, 10));

        JLabel t = new JLabel("Guest Details");
        t.setFont(F_HEAD);
        t.setForeground(TEXT);
        left(t);
        f.add(t);
        f.add(Box.createVerticalStrut(12));
        addField(f, "Full name", nameField);
        addField(f, "Phone (10 digits)", phoneField);
        addField(f, "Email", emailField);
        addField(f, "ID proof (Aadhaar / Passport no.)", idField);

        f.add(Box.createVerticalStrut(10));
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        f.add(sep);
        f.add(Box.createVerticalStrut(12));
        f.add(summaryRow("Room", sumRoom, false));
        f.add(summaryRow("Stay", sumStay, false));
        f.add(summaryRow("Subtotal", sumSub, false));
        f.add(summaryRow("GST (12%)", sumTax, false));
        f.add(Box.createVerticalStrut(6));
        f.add(summaryRow("TOTAL", sumTotal, true));
        f.add(Box.createVerticalGlue());
        f.add(Box.createVerticalStrut(14));

        RoundButton book = new RoundButton("Book Selected Room", PRIMARY, Color.WHITE);
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
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        p.add(l);
        p.add(Box.createVerticalStrut(4));
        p.add(tf);
        p.add(Box.createVerticalStrut(10));
    }

    private JPanel summaryRow(String key, JLabel value, boolean big) {
        JPanel r = new JPanel(new BorderLayout());
        r.setOpaque(false);
        JLabel k = new JLabel(key);
        k.setFont(big ? F_HEAD : F);
        k.setForeground(big ? TEXT : MUTED);
        value.setFont(big ? F_TOTAL : FB);
        value.setForeground(big ? PRIMARY : TEXT);
        r.add(k, BorderLayout.WEST);
        r.add(value, BorderLayout.EAST);
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, big ? 34 : 26));
        left(r);
        return r;
    }

    // ---------- bookings page ----------
    private JComponent buildBookingsPage() {
        JPanel page = new JPanel(new BorderLayout(0, 14));
        page.setOpaque(false);
        page.setBorder(new EmptyBorder(18, 28, 20, 28));

        JLabel t = new JLabel("All Bookings");
        t.setFont(F_HEAD);
        t.setForeground(TEXT);
        page.add(t, BorderLayout.NORTH);

        bookingTable.setRowHeight(40);
        bookingTable.setShowGrid(false);
        bookingTable.setShowHorizontalLines(true);
        bookingTable.setGridColor(new Color(0xF1F5F9));
        bookingTable.setIntercellSpacing(new Dimension(0, 1));
        bookingTable.setFont(F);
        bookingTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bookingTable.setSelectionBackground(PRIMARY_LIGHT);
        bookingTable.setSelectionForeground(TEXT);

        RowRenderer rowRenderer = new RowRenderer();
        bookingTable.setDefaultRenderer(Object.class, rowRenderer);
        bookingTable.getTableHeader().setDefaultRenderer(new HeaderRenderer());
        bookingTable.getTableHeader().setReorderingAllowed(false);
        bookingTable.getTableHeader().setPreferredSize(new Dimension(0, 40));
        bookingTable.getColumnModel().getColumn(0).setMaxWidth(80);
        bookingTable.getColumnModel().getColumn(2).setMaxWidth(80);

        bookingTable.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                int r = bookingTable.rowAtPoint(e.getPoint());
                if (r != rowRenderer.hoveredRow) {
                    rowRenderer.hoveredRow = r;
                    bookingTable.repaint();
                }
            }
        });
        bookingTable.addMouseListener(new MouseAdapter() {
            public void mouseExited(MouseEvent e) {
                if (rowRenderer.hoveredRow != -1) {
                    rowRenderer.hoveredRow = -1;
                    bookingTable.repaint();
                }
            }
        });

        JScrollPane sp = new JScrollPane(bookingTable);
        sp.setBorder(new LineBorder(BORDER, 1, true));
        sp.getViewport().setBackground(Color.WHITE);
        page.add(sp, BorderLayout.CENTER);

        RoundButton bill = new RoundButton("View Bill", NAVY2, Color.WHITE);
        bill.addActionListener(e -> { Booking b = selectedBooking(); if (b != null) showBill(b); });
        RoundButton cancel = new RoundButton("Cancel Selected Booking", RED, Color.WHITE);
        cancel.addActionListener(e -> cancelSelected());
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
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
        banner.setFont(F_SUBHEAD);
        banner.setBorder(new EmptyBorder(14, 14, 14, 14));

        String s = String.format(
                "Guest     : %s%nPhone     : %s%nEmail     : %s%nID proof  : %s%n%n"
                + "Room      : %d (%s)%nCheck-in  : %s%nCheck-out : %s%nNights    : %d%n%n"
                + "Rate      : Rs. %,.2f x %d%nSubtotal  : Rs. %,.2f%nGST (12%%) : Rs. %,.2f",
                b.getCustomer().getName(), b.getCustomer().getPhone(), b.getCustomer().getEmail(),
                b.getCustomer().getIdProof(), b.getRoom().getNumber(), b.getRoom().getType(),
                b.getCheckIn(), b.getCheckOut(), b.getNights(), b.getRoom().getPricePerNight(),
                b.getNights(), b.getSubtotal(), b.getTax());
        JTextArea body = new JTextArea(s);
        body.setFont(new Font("Consolas", Font.PLAIN, 13));
        body.setEditable(false);
        body.setForeground(TEXT);
        body.setBackground(new Color(0xF8FAFC));
        body.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel total = new JLabel(String.format("TOTAL   Rs. %,.2f", b.getTotal()), SwingConstants.CENTER);
        total.setOpaque(true);
        total.setBackground(NAVY);
        total.setForeground(new Color(0x93C5FD));
        total.setFont(F_TOTAL);
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
        f.setForeground(TEXT);
        f.setCaretColor(TEXT);
        f.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(7, 12, 7, 12)));
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                f.setBorder(new CompoundBorder(new LineBorder(PRIMARY, 1, true), new EmptyBorder(7, 12, 7, 12)));
            }
            public void focusLost(FocusEvent e) {
                f.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(7, 12, 7, 12)));
            }
        });
        return f;
    }

    static JLabel caption(String text) {
        JLabel l = new JLabel(text);
        l.setFont(F_CAPTION);
        l.setForeground(MUTED);
        return l;
    }

    static void left(JComponent c) { c.setAlignmentX(Component.LEFT_ALIGNMENT); }

    static Color typeColor(String type) {
        switch (type) {
            case "Single": return new Color(0x0284C7);
            case "Double": return new Color(0x4F46E5);
            case "Deluxe": return new Color(0x7C3AED);
            default: return PRIMARY;
        }
    }

    static Color lighten(Color c, float factor) {
        int r = Math.min(255, (int) (c.getRed() + (255 - c.getRed()) * factor));
        int g = Math.min(255, (int) (c.getGreen() + (255 - c.getGreen()) * factor));
        int b = Math.min(255, (int) (c.getBlue() + (255 - c.getBlue()) * factor));
        return new Color(r, g, b, c.getAlpha());
    }

    static Color darken(Color c, float factor) {
        int r = Math.max(0, (int) (c.getRed() * (1f - factor)));
        int g = Math.max(0, (int) (c.getGreen() * (1f - factor)));
        int b = Math.max(0, (int) (c.getBlue() * (1f - factor)));
        return new Color(r, g, b, c.getAlpha());
    }

    // ---------- custom components ----------
    static class RoundedPanel extends JPanel {
        private final Color fill, line;
        private final int arc;
        RoundedPanel(Color fill, Color line, int arc) {
            this.fill = fill;
            this.line = line;
            this.arc = arc;
            setOpaque(false);
        }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            if (line != null) {
                g2.setColor(line);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            }
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
            setFont(FB);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setRolloverEnabled(true);
            setBorder(new EmptyBorder(10, 20, 10, 20));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Color c = base;
            if (!isEnabled()) {
                c = new Color(0xCBD5E1);
            } else if (getModel().isPressed()) {
                c = darken(base, 0.15f);
            } else if (getModel().isRollover()) {
                c = lighten(base, 0.12f);
            }
            g2.setColor(c);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class NavButton extends JToggleButton {
        private boolean hovered = false;

        NavButton(String text) {
            super(text);
            setFont(FB);
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setRolloverEnabled(true);
            setBorder(new EmptyBorder(8, 16, 8, 16));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
            });
            addItemListener(e -> {
                setForeground(isSelected() ? Color.WHITE : new Color(0xCBD5E1));
                repaint();
            });
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            if (isSelected()) {
                g2.setColor(new Color(255, 255, 255, 35));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(PRIMARY_BORDER);
                g2.fillRoundRect(10, getHeight() - 3, getWidth() - 20, 3, 2, 2);
            } else if (hovered) {
                g2.setColor(new Color(255, 255, 255, 20));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class RoomCard extends JPanel {
        final Room room;
        final boolean available;
        boolean selected;
        private boolean hovered = false;

        RoomCard(Room room, boolean available, boolean selected, Consumer<Room> onClick) {
            this.room = room;
            this.available = available;
            this.selected = selected;
            setOpaque(false);
            setPreferredSize(new Dimension(185, 128));
            if (available) {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) { onClick.accept(room); }
                    public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
                });
            }
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth() - 2, h = getHeight() - 2;

            // Card background
            Color cardBg = !available ? new Color(0xF1F5F9) : selected ? PRIMARY_LIGHT : Color.WHITE;
            g2.setColor(cardBg);
            g2.fillRoundRect(1, 1, w, h, 16, 16);

            // Top accent stripe (clipped to rounded corners)
            Shape oldClip = g2.getClip();
            g2.setClip(new RoundRectangle2D.Float(1, 1, w, h, 16, 16));
            g2.setColor(available ? typeColor(room.getType()) : new Color(0xCBD5E1));
            g2.fillRect(1, 1, w, 6);
            g2.setClip(oldClip);

            // Card border
            if (selected) {
                g2.setStroke(new BasicStroke(2f));
                g2.setColor(PRIMARY);
            } else if (available && hovered) {
                g2.setStroke(new BasicStroke(1.5f));
                g2.setColor(PRIMARY_BORDER);
            } else {
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(BORDER);
            }
            g2.drawRoundRect(1, 1, w, h, 16, 16);

            // Room number
            g2.setColor(available ? TEXT : MUTED);
            g2.setFont(F_NUM_LARGE);
            g2.drawString(String.valueOf(room.getNumber()), 16, 44);

            // Room type
            g2.setColor(MUTED);
            g2.setFont(F);
            g2.drawString(room.getType() + " room", 16, 66);

            // Price per night
            g2.setColor(available ? TEXT : MUTED);
            g2.setFont(FB);
            g2.drawString(String.format("Rs. %,.0f / night", room.getPricePerNight()), 16, 92);

            // Status Pill
            String pill = !available ? "BOOKED" : selected ? "SELECTED" : "AVAILABLE";
            Color textC = !available ? RED : selected ? PRIMARY : GREEN;
            Color bgC = !available ? new Color(0xFEE2E2) : selected ? new Color(0xDBEAFE) : new Color(0xD1FAE5);

            g2.setFont(F_BADGE);
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(pill);
            int pw = tw + 16, ph = 20;
            int px = w - pw - 12, py = h - ph - 12;

            g2.setColor(bgC);
            g2.fillRoundRect(px, py, pw, ph, ph, ph);
            g2.setColor(textC);
            int textY = py + ((ph - fm.getHeight()) / 2) + fm.getAscent();
            g2.drawString(pill, px + 8, textY);
            g2.dispose();
        }
    }

    static class HeaderRenderer extends DefaultTableCellRenderer {
        HeaderRenderer() { setOpaque(true); }
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            super.getTableCellRendererComponent(t, v, false, false, r, c);
            setBackground(NAVY2);
            setForeground(Color.WHITE);
            setFont(FB);
            setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0x334155)),
                new EmptyBorder(0, 16, 0, 16)
            ));
            setPreferredSize(new Dimension(getPreferredSize().width, 40));
            return this;
        }
    }

    static class RowRenderer extends DefaultTableCellRenderer {
        int hoveredRow = -1;

        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean f, int r, int c) {
            super.getTableCellRendererComponent(t, v, sel, f, r, c);
            setBorder(new EmptyBorder(0, 16, 0, 16));
            setFont(F);
            if (sel) {
                setBackground(PRIMARY_LIGHT);
                setForeground(TEXT);
            } else if (r == hoveredRow) {
                setBackground(new Color(0xF1F5F9));
                setForeground(TEXT);
            } else {
                setBackground(r % 2 == 0 ? Color.WHITE : new Color(0xF8FAFC));
                setForeground(TEXT);
            }
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