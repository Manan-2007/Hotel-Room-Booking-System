package hotel.ui;

import hotel.model.Booking;
import hotel.model.Customer;
import hotel.model.Room;
import hotel.model.RoomType;
import hotel.service.BookingException;
import hotel.service.Hotel;
import hotel.service.Reports;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.DefaultFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.io.IOException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;

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
    private final Reports reports = new Reports(hotel);
    private JFrame frame;
    private Room selectedRoom;

    private final JPanel root = new JPanel(new BorderLayout());
    private final CardLayout pages = new CardLayout();
    private final JPanel pageHolder = new JPanel(pages);
    private NavButton navBook, navBookings, navReports;

    static final String ALL_TYPES = "All types";
    static final int CARD_MIN_W = 150, CARD_H = 128, GRID_GAP = 14;

    private final JSpinner inSpinner = dateSpinner();
    private final JSpinner nightsSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 30, 1));
    private final JComboBox<Object> typeFilter = new JComboBox<>(roomTypeOptions());
    private final JLabel outLabel = new JLabel();
    private final JLabel statusLabel = new JLabel();
    private final JPanel roomsGrid = new JPanel(new GridLayout(0, 4, GRID_GAP, GRID_GAP));

    private final JTextField nameField = field(""), phoneField = field(""), emailField = field(""), idField = field("");
    private final JLabel sumRoom = new JLabel("-"), sumStay = new JLabel("-"), sumSub = new JLabel("-"),
            sumTaxKey = new JLabel("GST"), sumTax = new JLabel("-"), sumTotal = new JLabel("Rs. 0.00");

    private final JLabel chipAvail = new JLabel("0"), chipActive = new JLabel("0");
    private final DefaultTableModel bookingModel = readOnlyModel(
            new String[]{"ID", "Customer", "Phone", "Room", "Check-in", "Check-out", "Total (Rs.)", "Status"},
            Integer.class, String.class, String.class, Integer.class, LocalDate.class, LocalDate.class, Double.class, String.class);
    private final JTable bookingTable = new JTable(bookingModel);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(bookingModel);
    private final JTextField searchField = field("");
    private RoundButton billButton, cancelButton;

    // reports page
    private final JLabel repRevenue = new JLabel(), repRevenueSub = new JLabel(), repBookings = new JLabel(),
            repBookingsSub = new JLabel(), repCancelled = new JLabel(), repCancelledSub = new JLabel(),
            repOccupied = new JLabel(), repOccupiedSub = new JLabel(), repAsOf = new JLabel();
    private final DefaultTableModel byTypeModel = readOnlyModel(new String[]{"Type", "Rooms", "Booked", "Revenue", "Share"},
            String.class, Long.class, Long.class, String.class, String.class);
    private final DefaultTableModel arrivalsModel = readOnlyModel(new String[]{"Check-in", "ID", "Guest", "Room", "Nights"},
            LocalDate.class, Integer.class, String.class, Integer.class, Long.class);

    /** The date the screen was last refreshed for; written on the Swing thread, read by the day watcher. */
    private volatile LocalDate shownDay = LocalDate.now();

    public HotelGUI() {
        root.setBackground(BG);
        root.add(buildHeader(), BorderLayout.NORTH);
        pageHolder.setOpaque(false);
        pageHolder.add(buildBookPage(), "book");
        pageHolder.add(buildBookingsPage(), "bookings");
        pageHolder.add(buildReportsPage(), "reports");
        root.add(pageHolder, BorderLayout.CENTER);

        inSpinner.addChangeListener(e -> refreshRooms());
        nightsSpinner.addChangeListener(e -> refreshRooms());
        typeFilter.addActionListener(e -> refreshRooms());
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applySearch(); }
            public void removeUpdate(DocumentEvent e) { applySearch(); }
            public void changedUpdate(DocumentEvent e) { applySearch(); }
        });
        refreshRooms();
        refreshBookings();
        refreshReports();
    }

    public JComponent getContent() { return root; }

    public void show() {
        frame = new JFrame("Grand Stay Hotel - Room Booking System");
        // save once more before exiting; save() is synchronized, so this also waits for any background save
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                try {
                    hotel.save();
                } catch (IOException ex) {
                    if (JOptionPane.showConfirmDialog(frame, "Bookings could not be saved (" + ex.getMessage()
                            + ").\nClose anyway?", "Save failed", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
                }
                System.exit(0);
            }
        });
        frame.setContentPane(root);
        // never open bigger than the usable screen area (e.g. 1366x768 laptops with a taskbar)
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        frame.setMinimumSize(new Dimension(Math.min(940, screen.width), Math.min(620, screen.height)));
        frame.setSize(Math.min(1100, screen.width), Math.min(760, screen.height));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        startDayWatcher();
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
        navReports = new NavButton("Reports");
        ButtonGroup bg = new ButtonGroup();
        bg.add(navBook);
        bg.add(navBookings);
        bg.add(navReports);
        navBook.addActionListener(e -> showPage("book"));
        navBookings.addActionListener(e -> showPage("bookings"));
        navReports.addActionListener(e -> showPage("reports"));
        navBook.setSelected(true);

        JPanel right = new JPanel(new GridBagLayout());
        right.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 6, 0, 6);
        right.add(chip(chipAvail, "Free for dates"), c);
        right.add(chip(chipActive, "Active bookings"), c);
        c.insets = new Insets(0, 24, 0, 0);
        right.add(navBook, c);
        c.insets = new Insets(0, 6, 0, 0);
        right.add(navBookings, c);
        right.add(navReports, c);

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
        switch (name) {
            case "bookings": navBookings.setSelected(true); refreshBookings(); break;
            case "reports": navReports.setSelected(true); refreshReports(); break;
            default: navBook.setSelected(true);
        }
    }

    // ---------- booking page ----------
    private JComponent buildBookPage() {
        JPanel page = new JPanel(new BorderLayout(18, 0));
        page.setOpaque(false);
        page.setBorder(new EmptyBorder(18, 28, 20, 28));

        // search bar (sits above the room cards so the guest form gets the full height)
        RoundedPanel bar = new RoundedPanel(Color.WHITE, BORDER, 14);
        bar.setLayout(new BoxLayout(bar, BoxLayout.Y_AXIS));
        bar.setBorder(new EmptyBorder(10, 14, 10, 14));

        JPanel row1 = barRow();
        row1.add(caption("Check-in"));
        styleSpinner(inSpinner, 122, false);
        inSpinner.setToolTipText("Type a date (YYYY-MM-DD) and press Enter, or use the arrows");
        row1.add(inSpinner);
        row1.add(Box.createHorizontalStrut(6));
        row1.add(caption("Nights"));
        styleSpinner(nightsSpinner, 64, true);
        row1.add(nightsSpinner);
        row1.add(Box.createHorizontalStrut(6));
        row1.add(caption("Room type"));
        typeFilter.setFont(F);
        typeFilter.setFocusable(false);
        row1.add(typeFilter);

        JPanel row2 = barRow();
        outLabel.setFont(FB);
        outLabel.setForeground(TEXT);
        statusLabel.setFont(FB);
        row2.add(outLabel);
        row2.add(statusLabel);

        bar.add(row1);
        bar.add(Box.createVerticalStrut(8));
        bar.add(row2);

        // room cards: width follows the window, column count adapts to the space
        roomsGrid.setOpaque(false);
        TrackWidthPanel wrap = new TrackWidthPanel(new BorderLayout());
        wrap.setBorder(new EmptyBorder(2, 2, 8, 8));
        wrap.add(roomsGrid, BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(wrap, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.getViewport().addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) { updateGridColumns(sp.getViewport().getWidth()); }
        });

        JPanel leftCol = new JPanel(new BorderLayout(0, 14));
        leftCol.setOpaque(false);
        leftCol.add(bar, BorderLayout.NORTH);
        leftCol.add(sp, BorderLayout.CENTER);

        page.add(leftCol, BorderLayout.CENTER);
        page.add(buildForm(), BorderLayout.EAST);
        return page;
    }

    /** A left-aligned row for the search bar that never grows taller than its content. */
    private static JPanel barRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)) {
            public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
        };
        row.setOpaque(false);
        left(row);
        return row;
    }

    private void updateGridColumns(int width) {
        GridLayout gl = (GridLayout) roomsGrid.getLayout();
        int cols = Math.max(2, Math.min(5, (width - 10 + GRID_GAP) / (CARD_MIN_W + GRID_GAP)));
        if (cols != gl.getColumns()) {
            gl.setColumns(cols);
            roomsGrid.revalidate();
        }
    }

    private JComponent buildForm() {
        RoundedPanel f = new RoundedPanel(Color.WHITE, BORDER, 16);
        f.setLayout(new BorderLayout(0, 12));
        f.setBorder(new EmptyBorder(20, 20, 20, 20));
        f.setPreferredSize(new Dimension(300, 10));

        JLabel t = new JLabel("Guest Details");
        t.setFont(F_HEAD);
        t.setForeground(TEXT);
        f.add(t, BorderLayout.NORTH);

        // fields scroll if the window is short; the bill and Book button below always stay visible
        TrackWidthPanel fields = new TrackWidthPanel(null);
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));
        addField(fields, "Full name", nameField);
        addField(fields, "Phone (10 digits)", phoneField);
        addField(fields, "Email", emailField);
        addField(fields, "ID proof (Aadhaar / Passport no.)", idField);
        JScrollPane fsp = new JScrollPane(fields, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        fsp.setBorder(null);
        fsp.setOpaque(false);
        fsp.getViewport().setOpaque(false);
        fsp.getVerticalScrollBar().setUnitIncrement(16);
        f.add(fsp, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        left(sep);
        bottom.add(sep);
        bottom.add(Box.createVerticalStrut(12));
        bottom.add(summaryRow(new JLabel("Room"), sumRoom, false));
        bottom.add(summaryRow(new JLabel("Stay"), sumStay, false));
        bottom.add(summaryRow(new JLabel("Subtotal"), sumSub, false));
        bottom.add(summaryRow(sumTaxKey, sumTax, false));
        bottom.add(Box.createVerticalStrut(6));
        bottom.add(summaryRow(new JLabel("TOTAL"), sumTotal, true));
        bottom.add(Box.createVerticalStrut(14));

        RoundButton book = new RoundButton("Book Selected Room", PRIMARY, Color.WHITE);
        book.addActionListener(e -> book());
        book.setAlignmentX(Component.LEFT_ALIGNMENT);
        book.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        bottom.add(book);
        f.add(bottom, BorderLayout.SOUTH);
        return f;
    }

    private void addField(JPanel p, String cap, JTextField tf) {
        JLabel l = caption(cap);
        left(l);
        left(tf);
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        // keep the focused field visible when the form is scrolled
        tf.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { p.scrollRectToVisible(tf.getBounds()); }
        });
        p.add(l);
        p.add(Box.createVerticalStrut(4));
        p.add(tf);
        p.add(Box.createVerticalStrut(10));
    }

    private JPanel summaryRow(JLabel k, JLabel value, boolean big) {
        JPanel r = new JPanel(new BorderLayout());
        r.setOpaque(false);
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
        JPanel search = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        search.setOpaque(false);
        search.add(caption("Search name, phone or booking ID"));
        searchField.setColumns(18);
        search.add(searchField);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(t, BorderLayout.WEST);
        top.add(search, BorderLayout.EAST);
        page.add(top, BorderLayout.NORTH);

        // click a header to sort; newest bookings first by default
        bookingTable.setRowSorter(sorter);
        sorter.setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.DESCENDING)));

        RowRenderer rowRenderer = styleTable(bookingTable);
        fixedWidths(bookingTable, 80, -1, -1, 80);

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
            public void mouseClicked(MouseEvent e) {
                // double-click a row to open its bill
                if (e.getClickCount() == 2) { Booking b = bookingAtSelection(); if (b != null) showBill(b); }
            }
        });
        bookingTable.getSelectionModel().addListSelectionListener(e -> updateBookingButtons());

        page.add(tableScroll(bookingTable), BorderLayout.CENTER);

        billButton = new RoundButton("View Bill", NAVY2, Color.WHITE);
        billButton.addActionListener(e -> { Booking b = selectedBooking(); if (b != null) showBill(b); });
        cancelButton = new RoundButton("Cancel Selected Booking", RED, Color.WHITE);
        cancelButton.addActionListener(e -> cancelSelected());
        JLabel hint = caption("Only upcoming bookings can be cancelled. Double-click a row to view its bill.");
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        btns.setOpaque(false);
        btns.add(billButton);
        btns.add(cancelButton);
        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(hint, BorderLayout.WEST);
        south.add(btns, BorderLayout.EAST);
        page.add(south, BorderLayout.SOUTH);
        updateBookingButtons();
        return page;
    }

    // ---------- reports page ----------
    private JComponent buildReportsPage() {
        JPanel page = new JPanel(new BorderLayout(0, 14));
        page.setOpaque(false);
        page.setBorder(new EmptyBorder(18, 28, 20, 28));

        JLabel t = new JLabel("Reports");
        t.setFont(F_HEAD);
        t.setForeground(TEXT);
        repAsOf.setFont(F_CAPTION);
        repAsOf.setForeground(MUTED);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(t, BorderLayout.WEST);
        top.add(repAsOf, BorderLayout.EAST);

        JPanel tiles = new JPanel(new GridLayout(1, 4, 14, 0));
        tiles.setOpaque(false);
        tiles.add(tile("Booked revenue", repRevenue, repRevenueSub));
        tiles.add(tile("Bookings", repBookings, repBookingsSub));
        tiles.add(tile("Cancelled", repCancelled, repCancelledSub));
        tiles.add(tile("Occupied tonight", repOccupied, repOccupiedSub));

        JPanel north = new JPanel(new BorderLayout(0, 14));
        north.setOpaque(false);
        north.add(top, BorderLayout.NORTH);
        north.add(tiles, BorderLayout.CENTER);
        page.add(north, BorderLayout.NORTH);

        JTable byType = new JTable(byTypeModel);
        styleTable(byType);
        fixedWidths(byType, 84, 78, 86, -1, 70);
        JTable arrivals = new JTable(arrivalsModel);
        styleTable(arrivals);
        fixedWidths(arrivals, 108, 62, -1, 70, 80);

        JPanel tables = new JPanel(new GridLayout(1, 2, 14, 0));
        tables.setOpaque(false);
        tables.add(titled("Revenue by room type", tableScroll(byType)));
        tables.add(titled("Check-ins in the next 7 days", tableScroll(arrivals)));
        page.add(tables, BorderLayout.CENTER);
        return page;
    }

    private JComponent tile(String caption, JLabel value, JLabel sub) {
        RoundedPanel p = new RoundedPanel(Color.WHITE, BORDER, 14);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(14, 16, 14, 16));
        JLabel cap = caption(caption);
        value.setFont(F_TOTAL);
        value.setForeground(TEXT);
        sub.setFont(F_CAPTION);
        sub.setForeground(MUTED);
        p.add(cap);
        p.add(Box.createVerticalStrut(6));
        p.add(value);
        p.add(Box.createVerticalStrut(4));
        p.add(sub);
        return p;
    }

    private static JComponent titled(String title, JComponent body) {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);
        JLabel l = new JLabel(title);
        l.setFont(F_SUBHEAD);
        l.setForeground(TEXT);
        p.add(l, BorderLayout.NORTH);
        p.add(body, BorderLayout.CENTER);
        return p;
    }

    private void refreshReports() {
        repAsOf.setText("As of " + LocalDate.now());
        repRevenue.setText(String.format("Rs. %,.0f", reports.totalRevenue()));
        repRevenueSub.setText("incl. GST, excl. cancelled");
        repBookings.setText(String.valueOf(reports.keptBookings()));
        repBookingsSub.setText(String.format("average stay %.1f night(s)", reports.averageNights()));
        repCancelled.setText(String.valueOf(reports.cancelledBookings()));
        repCancelledSub.setText(String.format("%.0f%% of %d booking(s)", reports.cancellationRate() * 100, reports.totalBookings()));
        long occ = reports.roomsOccupiedTonight(), total = reports.totalRooms();
        repOccupied.setText(String.format("%.0f%%", total == 0 ? 0 : occ * 100.0 / total));
        repOccupiedSub.setText(occ + " of " + total + " rooms");

        Map<RoomType, Long> rooms = reports.roomsByType(), count = reports.bookingsByType();
        Map<RoomType, Double> revenue = reports.revenueByType();
        double all = reports.totalRevenue();
        byTypeModel.setRowCount(0);
        for (RoomType type : RoomType.values()) {
            double rev = revenue.getOrDefault(type, 0.0);
            byTypeModel.addRow(new Object[]{type.getLabel(), rooms.getOrDefault(type, 0L), count.getOrDefault(type, 0L),
                    String.format("Rs. %,.0f", rev), all == 0 ? "-" : String.format("%.0f%%", rev * 100 / all)});
        }
        arrivalsModel.setRowCount(0);
        for (Booking b : reports.upcomingCheckIns(7))
            arrivalsModel.addRow(new Object[]{b.getCheckIn(), b.getId(), b.getCustomer().getName(),
                    b.getRoom().getNumber(), b.getNights()});
    }

    // ---------- logic ----------
    private LocalDate[] dates() {
        Object v = inSpinner.getValue();
        if (!(v instanceof Date)) return null;
        LocalDate in = toLocal((Date) v);
        if (in.isBefore(LocalDate.now())) return null;
        return new LocalDate[]{in, in.plusDays((Integer) nightsSpinner.getValue())};
    }

    /** "All types" followed by every RoomType. */
    private static Object[] roomTypeOptions() {
        List<Object> options = new ArrayList<>();
        options.add(ALL_TYPES);
        options.addAll(Arrays.asList(RoomType.values()));
        return options.toArray();
    }

    private void refreshRooms() {
        LocalDate[] d = dates();
        roomsGrid.removeAll();
        if (d == null) {
            // clear everything so stale availability is never shown
            selectedRoom = null;
            outLabel.setText("");
            statusLabel.setText("Choose a valid check-in date (today or later)");
            statusLabel.setForeground(RED);
            chipAvail.setText("-");
            roomsGrid.revalidate();
            roomsGrid.repaint();
            updateSummary();
            return;
        }
        outLabel.setText("Check-out " + d[1] + "  •");
        Object chosen = typeFilter.getSelectedItem();
        RoomType type = chosen instanceof RoomType ? (RoomType) chosen : null;
        boolean allTypes = type == null;
        int freeAll = 0, freeShown = 0, shown = 0;
        boolean selectionStillFree = false;
        for (Room r : hotel.getAllRooms()) {
            boolean ok = hotel.isAvailable(r, d[0], d[1]);
            if (ok) freeAll++;
            if (!allTypes && r.getType() != type) continue;
            shown++;
            if (ok) freeShown++;
            boolean sel = ok && selectedRoom != null && selectedRoom.getNumber() == r.getNumber();
            if (sel) selectionStillFree = true;
            roomsGrid.add(new RoomCard(r, ok, sel, this::selectRoom));
        }
        if (!selectionStillFree) selectedRoom = null;
        statusLabel.setText(freeShown + " of " + shown + (allTypes ? "" : " " + type) + " rooms available");
        statusLabel.setForeground(freeShown > 0 ? GREEN : RED);
        chipAvail.setText(String.valueOf(freeAll));
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
            sumTaxKey.setText("GST");
            sumTax.setText("-");
            sumTotal.setText("Rs. 0.00");
            return;
        }
        long n = d[1].toEpochDay() - d[0].toEpochDay();
        double price = selectedRoom.getPricePerNight();
        double sub = n * price, tax = sub * Booking.taxRateFor(price);
        sumRoom.setText(selectedRoom.getNumber() + " (" + selectedRoom.getType() + ")");
        sumStay.setText(n + " night(s)");
        sumSub.setText(String.format("Rs. %,.2f", sub));
        sumTaxKey.setText(Booking.taxLabelFor(price));
        sumTax.setText(String.format("Rs. %,.2f", tax));
        sumTotal.setText(String.format("Rs. %,.2f", sub + tax));
    }

    private void book() {
        LocalDate[] d = dates();
        if (d == null) { error("Please choose a valid check-in date (today or later)."); return; }
        if (selectedRoom == null) { error("Click an available room card first."); return; }

        String name = nameField.getText().trim(), phone = phoneField.getText().trim(),
                email = emailField.getText().trim(), id = idField.getText().trim();
        List<String> problems = new ArrayList<>();
        if (!Customer.isValidName(name)) problems.add("Full name: letters only, at least 2");
        if (!Customer.isValidPhone(phone)) problems.add("Phone: exactly 10 digits");
        if (!Customer.isValidEmail(email)) problems.add("Email: e.g. name@example.com");
        if (!Customer.isValidIdProof(id)) problems.add("ID proof: 4-24 letters or digits");
        if (!problems.isEmpty()) {
            error("Please fix the following:\n• " + String.join("\n• ", problems));
            return;
        }

        Booking b;
        try {
            b = hotel.book(new Customer(name, phone, email, id), hotel.findRoom(selectedRoom.getNumber()), d[0], d[1]);
        } catch (BookingException ex) {
            // one catch covers both InvalidBookingException and RoomNotAvailableException
            error(ex.getMessage());
            refreshRooms();
            return;
        }
        saveInBackground();
        nameField.setText(""); phoneField.setText(""); emailField.setText(""); idField.setText("");
        selectedRoom = null;
        refreshRooms();
        refreshBookings();
        refreshReports();
        showBill(b);
    }

    /** The booking in the selected table row, or null (no message). */
    private Booking bookingAtSelection() {
        int row = bookingTable.getSelectedRow();
        if (row < 0) return null;
        // the view may be sorted/filtered, so map back to the model row
        int modelRow = bookingTable.convertRowIndexToModel(row);
        return hotel.findBooking((Integer) bookingModel.getValueAt(modelRow, 0));
    }

    private Booking selectedBooking() {
        Booking b = bookingAtSelection();
        if (b == null) error("Select a booking from the table first.");
        return b;
    }

    private void updateBookingButtons() {
        if (billButton == null) return;
        Booking b = bookingAtSelection();
        billButton.setEnabled(b != null);
        cancelButton.setEnabled(b != null && b.canCancel());
    }

    private void cancelSelected() {
        Booking b = selectedBooking();
        if (b == null) return;
        if (b.isCancelled()) { error("This booking is already cancelled."); return; }
        if (!b.canCancel()) {
            error("This stay has already started or finished (" + b.getStatus() + "), so it can't be cancelled.");
            return;
        }
        if (JOptionPane.showConfirmDialog(frame, "Cancel booking " + b.getId() + " for "
                + b.getCustomer().getName() + "?", "Confirm cancellation", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            hotel.cancel(b.getId());
            saveInBackground();
            refreshBookings();
            refreshRooms();
            refreshReports();
        }
    }

    private void refreshBookings() {
        bookingModel.setRowCount(0);
        int active = 0;
        for (Booking b : hotel.getBookings()) {
            if (b.isActive()) active++;
            bookingModel.addRow(new Object[]{b.getId(), b.getCustomer().getName(), b.getCustomer().getPhone(),
                    b.getRoom().getNumber(), b.getCheckIn(), b.getCheckOut(), b.getTotal(), b.getStatus()});
        }
        chipActive.setText(String.valueOf(active));
        updateBookingButtons();
    }

    private void applySearch() {
        String q = searchField.getText().trim();
        // search the ID, customer and phone columns, ignoring case
        sorter.setRowFilter(q.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(q), 0, 1, 2));
    }

    // ---------- background work ----------
    /** Writes bookings.txt on a worker thread so a slow disk never freezes the window. */
    private void saveInBackground() {
        new SwingWorker<Void, Void>() {
            protected Void doInBackground() throws IOException {
                hotel.save();            // runs on a background thread
                return null;
            }
            protected void done() {      // runs back on the Swing thread, so it may show a dialog
                try {
                    get();
                } catch (ExecutionException ex) {
                    error("Your booking is made, but it could not be saved to disk:\n" + ex.getCause().getMessage());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    /**
     * Statuses (Upcoming -> Ongoing -> Completed) and the earliest allowed check-in depend on today's date.
     * A background thread checks once a minute; when the date has changed it asks the Swing thread to refresh.
     * Checking every minute (not one timer for midnight) still works after the laptop wakes from sleep.
     */
    private void startDayWatcher() {
        ScheduledExecutorService watcher = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread t = new Thread(task, "day-watcher");
            t.setDaemon(true);   // don't keep the program alive after the window closes
            return t;
        });
        watcher.scheduleAtFixedRate(() -> {
            if (!LocalDate.now().equals(shownDay)) SwingUtilities.invokeLater(this::onNewDay);
        }, 1, 1, TimeUnit.MINUTES);
    }

    /** Called on the Swing thread when the date changes. */
    void onNewDay() {
        LocalDate today = LocalDate.now();
        if (today.equals(shownDay)) return;
        shownDay = today;
        SpinnerDateModel m = (SpinnerDateModel) inSpinner.getModel();
        m.setStart(toDate(today));
        if (toLocal((Date) m.getValue()).isBefore(today)) m.setValue(toDate(today));
        refreshRooms();
        refreshBookings();
        refreshReports();
    }

    // ---------- receipt ----------
    JComponent receipt(Booking b) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        String status = b.getStatus();
        String title = b.isCancelled() ? "BOOKING CANCELLED" : "Completed".equals(status) ? "STAY COMPLETED" : "BOOKING CONFIRMED";
        JLabel banner = new JLabel(title + "   #" + b.getId(), SwingConstants.CENTER);
        banner.setOpaque(true);
        banner.setBackground(b.isCancelled() ? RED : "Completed".equals(status) ? MUTED : GREEN);
        banner.setForeground(Color.WHITE);
        banner.setFont(F_SUBHEAD);
        banner.setBorder(new EmptyBorder(14, 14, 14, 14));

        String s = String.format(
                "Guest     : %s%nPhone     : %s%nEmail     : %s%nID proof  : %s%n%n"
                + "Room      : %d (%s)%nCheck-in  : %s%nCheck-out : %s%nNights    : %d%n%n"
                + "Rate      : Rs. %,.2f x %d%nSubtotal  : Rs. %,.2f%n%-10s: Rs. %,.2f",
                b.getCustomer().getName(), b.getCustomer().getPhone(), b.getCustomer().getEmail(),
                b.getCustomer().getMaskedIdProof(), b.getRoom().getNumber(), b.getRoom().getType(),
                b.getCheckIn(), b.getCheckOut(), b.getNights(), b.getRoom().getPricePerNight(),
                b.getNights(), b.getSubtotal(), Booking.taxLabelFor(b.getRoom().getPricePerNight()), b.getTax());
        JTextArea body = new JTextArea(s);
        body.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13)); // monospaced on every OS so the colons line up
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
        p.setPreferredSize(new Dimension(400, 400));
        return p;
    }

    private void showBill(Booking b) {
        JOptionPane.showMessageDialog(frame, receipt(b), "Booking Receipt", JOptionPane.PLAIN_MESSAGE);
    }

    private void error(String msg) { JOptionPane.showMessageDialog(frame, msg, "Please check", JOptionPane.WARNING_MESSAGE); }

    // ---------- date helpers ----------
    static JSpinner dateSpinner() {
        Date today = toDate(LocalDate.now());
        SpinnerDateModel m = new SpinnerDateModel(toDate(LocalDate.now().plusDays(1)), today, null, Calendar.DAY_OF_MONTH);
        JSpinner s = new JSpinner(m);
        s.setEditor(new JSpinner.DateEditor(s, "yyyy-MM-dd"));
        return s;
    }

    static Date toDate(LocalDate d) { return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant()); }
    static LocalDate toLocal(Date d) { return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(); }

    static void styleSpinner(JSpinner s, int width, boolean liveCommit) {
        s.setFont(F);
        if (s.getEditor() instanceof JSpinner.DefaultEditor) {
            JFormattedTextField tf = ((JSpinner.DefaultEditor) s.getEditor()).getTextField();
            tf.setFont(F);
            tf.setForeground(TEXT);
            tf.setBorder(new EmptyBorder(4, 6, 4, 6));
            // number spinner updates while typing; the date one waits for Enter so half-typed dates don't jump
            if (liveCommit && tf.getFormatter() instanceof DefaultFormatter)
                ((DefaultFormatter) tf.getFormatter()).setCommitsOnValidEdit(true);
        }
        s.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(2, 4, 2, 4)));
        s.setPreferredSize(new Dimension(width, 36));
    }

    // ---------- table helpers ----------
    /** A table model whose cells can't be edited and whose columns sort by real type (number, date). */
    static DefaultTableModel readOnlyModel(String[] columns, Class<?>... types) {
        return new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
            public Class<?> getColumnClass(int c) { return types[c]; }
        };
    }

    /** Shared look for every table; returns the row renderer so callers can add hover effects. */
    static RowRenderer styleTable(JTable t) {
        t.setRowHeight(40);
        t.setShowGrid(false);
        t.setShowHorizontalLines(true);
        t.setGridColor(new Color(0xF1F5F9));
        t.setIntercellSpacing(new Dimension(0, 1));
        t.setFont(F);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setSelectionBackground(PRIMARY_LIGHT);
        t.setSelectionForeground(TEXT);
        t.setFillsViewportHeight(true);
        RowRenderer rowRenderer = new RowRenderer();
        t.setDefaultRenderer(Object.class, rowRenderer);
        t.setDefaultRenderer(Number.class, rowRenderer);
        t.setDefaultRenderer(Double.class, rowRenderer);
        t.getTableHeader().setDefaultRenderer(new HeaderRenderer());
        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setPreferredSize(new Dimension(0, 40));
        return rowRenderer;
    }

    /** Gives columns a fixed width (-1 = flexible); flexible columns share whatever space is left. */
    static void fixedWidths(JTable t, int... widths) {
        for (int i = 0; i < widths.length; i++) {
            if (widths[i] <= 0) continue;
            javax.swing.table.TableColumn col = t.getColumnModel().getColumn(i);
            col.setMinWidth(widths[i]);
            col.setMaxWidth(widths[i]);
            col.setPreferredWidth(widths[i]);
        }
    }

    static JScrollPane tableScroll(JTable t) {
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(new LineBorder(BORDER, 1, true));
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

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

    static Color typeColor(RoomType type) {
        switch (type) {
            case SINGLE: return new Color(0x0284C7);
            case DOUBLE: return new Color(0x4F46E5);
            case DELUXE: return new Color(0x7C3AED);
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
            setForeground(new Color(0xCBD5E1));
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
            setPreferredSize(new Dimension(CARD_MIN_W, CARD_H));
            setToolTipText(String.format("Room %d - %s - Rs. %,.0f / night%s", room.getNumber(), room.getType(),
                    room.getPricePerNight(), available ? "" : " (booked for these dates)"));
            if (available) {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                setFocusable(true);
                addMouseListener(new MouseAdapter() {
                    // mousePressed (not mouseClicked) so a tiny mouse movement doesn't swallow the click
                    public void mousePressed(MouseEvent e) { requestFocusInWindow(); onClick.accept(room); }
                    public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
                });
                addFocusListener(new FocusAdapter() {
                    public void focusGained(FocusEvent e) { repaint(); }
                    public void focusLost(FocusEvent e) { repaint(); }
                });
                // keyboard: Tab to a card, Space/Enter to select it
                getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("SPACE"), "pick");
                getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ENTER"), "pick");
                getActionMap().put("pick", new AbstractAction() {
                    public void actionPerformed(ActionEvent e) { onClick.accept(room); }
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
            } else if (available && isFocusOwner()) {
                g2.setStroke(new BasicStroke(2f));
                g2.setColor(PRIMARY_BORDER);
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
            if (t != null && t.getRowSorter() != null) {
                for (RowSorter.SortKey k : t.getRowSorter().getSortKeys())
                    if (k.getColumn() == t.convertColumnIndexToModel(c) && k.getSortOrder() != SortOrder.UNSORTED)
                        setText(v + (k.getSortOrder() == SortOrder.ASCENDING ? "  \u2191" : "  \u2193"));
            }
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

    static Color statusColor(String status) {
        switch (status) {
            case "Cancelled": return RED;
            case "Completed": return MUTED;
            case "Ongoing": return PRIMARY;
            default: return GREEN;
        }
    }

    /** Panel that always matches the scroll pane's width, so content wraps/shrinks instead of scrolling sideways. */
    static class TrackWidthPanel extends JPanel implements Scrollable {
        TrackWidthPanel(LayoutManager lm) { super(lm); setOpaque(false); }
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) {
            return o == SwingConstants.VERTICAL ? r.height : r.width;
        }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
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
            if (v instanceof Double) setText(String.format("%,.2f", (Double) v));
            if ("Status".equals(t.getColumnName(c))) {
                setFont(FB);
                setForeground(statusColor(String.valueOf(v)));
            }
            return this;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new HotelGUI().show());
    }
}