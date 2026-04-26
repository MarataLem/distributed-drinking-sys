package server;

import common.BranchSales;
import common.Drink;
import common.Order;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class HeadquarterGUI extends JFrame {

    // ── Shared state ──────────────────────────────────────
    private static final Map<String, BranchSales> salesMap = new ConcurrentHashMap<>();
    private static final Map<String, List<Drink>>  stockMap = new ConcurrentHashMap<>();
    private static final int PORT = 5000;
    private static final int LOW_STOCK_THRESHOLD = 5;

    // ── GUI Components ────────────────────────────────────
    private JTextArea logArea;
    private JTable    reportTable;
    private DefaultTableModel tableModel;
    private JLabel    totalSalesLabel;
    private JLabel    statusLabel;

    public HeadquarterGUI() {
        initStock();
        buildUI();
        startServer();
    }

    // ── Build the UI ──────────────────────────────────────
    private void buildUI() {
        setTitle("DRINKS SYSTEM — NAIROBI HEADQUARTER");
        setSize(900, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(30, 30, 30));
        setLayout(new BorderLayout(10, 10));

        // ── TOP PANEL — Title ──
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(0, 102, 51));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JLabel title = new JLabel("NAIROBI HQ — DRINKS ORDERING SYSTEM");
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setForeground(Color.WHITE);

        statusLabel = new JLabel("Server Status: Starting...");
        statusLabel.setForeground(Color.YELLOW);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 13));

        topPanel.add(title, BorderLayout.WEST);
        topPanel.add(statusLabel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // ── CENTER PANEL — Split View ──
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(440);
        splitPane.setBackground(new Color(30, 30, 30));

        // LEFT — Live Log
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBackground(new Color(30, 30, 30));
        logPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GREEN),
            "Live Activity Log",
            0, 0, new Font("Arial", Font.BOLD, 13), Color.GREEN));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(new Color(20, 20, 20));
        logArea.setForeground(Color.GREEN);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setLineWrap(true);

        JScrollPane logScroll = new JScrollPane(logArea);
        logPanel.add(logScroll, BorderLayout.CENTER);

        // RIGHT — Reports Table
        JPanel reportPanel = new JPanel(new BorderLayout());
        reportPanel.setBackground(new Color(30, 30, 30));
        reportPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.CYAN),
            "Orders Report",
            0, 0, new Font("Arial", Font.BOLD, 13), Color.CYAN));

        String[] columns = {"Customer", "Drink", "Qty", "Branch", "Amount (KES)"};
        tableModel = new DefaultTableModel(columns, 0);
        reportTable = new JTable(tableModel);
        reportTable.setBackground(new Color(40, 40, 40));
        reportTable.setForeground(Color.WHITE);
        reportTable.setGridColor(Color.GRAY);
        reportTable.getTableHeader().setBackground(new Color(0, 102, 51));
        reportTable.getTableHeader().setForeground(Color.WHITE);
        reportTable.setRowHeight(25);

        JScrollPane tableScroll = new JScrollPane(reportTable);
        reportPanel.add(tableScroll, BorderLayout.CENTER);

        splitPane.setLeftComponent(logPanel);
        splitPane.setRightComponent(reportPanel);
        add(splitPane, BorderLayout.CENTER);

        // ── BOTTOM PANEL — Buttons + Total ──
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(new Color(40, 40, 40));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnPanel.setBackground(new Color(40, 40, 40));

        JButton btnOrders  = makeButton("Orders by Branch",  new Color(0, 102, 204));
        JButton btnSales   = makeButton("Sales by Branch",   new Color(153, 0, 76));
        JButton btnStock   = makeButton("Stock Levels",      new Color(102, 51, 0));
        JButton btnClear   = makeButton("Clear Log",         new Color(80, 80, 80));

        btnOrders.addActionListener(e -> showOrdersByBranch());
        btnSales .addActionListener(e -> showSalesByBranch());
        btnStock .addActionListener(e -> showStock());
        btnClear .addActionListener(e -> logArea.setText(""));

        btnPanel.add(btnOrders);
        btnPanel.add(btnSales);
        btnPanel.add(btnStock);
        btnPanel.add(btnClear);

        // Total Sales Label
        totalSalesLabel = new JLabel("Total Sales: KES 0.00");
        totalSalesLabel.setForeground(Color.YELLOW);
        totalSalesLabel.setFont(new Font("Arial", Font.BOLD, 15));

        bottomPanel.add(btnPanel,        BorderLayout.WEST);
        bottomPanel.add(totalSalesLabel, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    // ── Helper: make styled button ────────────────────────
    private JButton makeButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Arial", Font.BOLD, 12));
        btn.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        return btn;
    }

    // ── Log a message to the text area ───────────────────
    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    // ── Update total sales label ──────────────────────────
    private void updateTotal() {
        double total = salesMap.values().stream()
                               .mapToDouble(BranchSales::getTotalSales).sum();
        SwingUtilities.invokeLater(() ->
            totalSalesLabel.setText(String.format("Total Sales: KES %.2f", total)));
    }

    // ── Start server in background thread ────────────────
    private void startServer() {
        new Thread(() -> {
            try {
                ServerSocket ss = new ServerSocket(PORT);
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Server Status: RUNNING on port " + PORT);
                    statusLabel.setForeground(Color.GREEN);
                });
                log("=== HQ Server started on port " + PORT + " ===");
                log("Waiting for branches to connect...\n");
                while (true) {
                    Socket client = ss.accept();
                    new Thread(() -> handleBranch(client)).start();
                }
            } catch (IOException e) {
                log("ERROR starting server: " + e.getMessage());
            }
        }).start();
    }

    // ── Handle a branch connection ────────────────────────
    private void handleBranch(Socket socket) {
        try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream  in  = new ObjectInputStream(socket.getInputStream())) {

            String branch = (String) in.readObject();
            log("[CONNECTED] Branch: " + branch + " (" + socket.getInetAddress() + ")");

            out.writeObject(stockMap.get(branch));
            out.flush();

            while (true) {
                Object obj = in.readObject();
                if ("DISCONNECT".equals(obj)) {
                    log("[DISCONNECTED] Branch: " + branch);
                    break;
                }
                Order order = (Order) obj;
                processOrder(order, out);
            }
        } catch (Exception e) {
            log("[DISCONNECTED] A branch client disconnected.");
        }
    }

    // ── Process order ─────────────────────────────────────
    private synchronized void processOrder(Order order,
                                            ObjectOutputStream out) throws IOException {
        String branch = order.getBranch();
        List<Drink> drinks = stockMap.get(branch);
        boolean found = false;

        for (Drink d : drinks) {
            if (d.getName().equalsIgnoreCase(order.getDrinkName())) {
                if (d.getStockLevel() >= order.getQuantity()) {
                    d.setStockLevel(d.getStockLevel() - order.getQuantity());
                    salesMap.get(branch).addOrder(order);

                    log("[ORDER] " + order.getCustomerName()
                      + " | " + order.getDrinkName()
                      + " x" + order.getQuantity()
                      + " | " + branch
                      + " | KES " + order.getTotalAmount());

                    // Add row to table
                    SwingUtilities.invokeLater(() ->
                        tableModel.addRow(new Object[]{
                            order.getCustomerName(),
                            order.getDrinkName(),
                            order.getQuantity(),
                            order.getBranch(),
                            String.format("%.2f", order.getTotalAmount())
                        })
                    );

                    updateTotal();
                    out.writeObject("SUCCESS: Order placed!");

                    if (d.getStockLevel() < LOW_STOCK_THRESHOLD) {
                        String alert = "LOW STOCK: " + d.getName()
                                     + " at " + branch
                                     + " — only " + d.getStockLevel() + " left!";
                        log("⚠ " + alert);
                        out.writeObject("ALERT:" + alert);
                    }
                } else {
                    out.writeObject("ERROR: Not enough stock. Available: " + d.getStockLevel());
                }
                found = true;
                break;
            }
        }
        if (!found) out.writeObject("ERROR: Drink not found.");
        out.flush();
    }

    // ── Report Buttons ────────────────────────────────────
    private void showOrdersByBranch() {
        StringBuilder sb = new StringBuilder();
        salesMap.forEach((branch, bs) -> {
            sb.append("\n--- ").append(branch).append(" ---\n");
            if (bs.getOrders().isEmpty()) sb.append("  No orders yet.\n");
            else bs.getOrders().forEach(o -> sb.append("  ").append(o).append("\n"));
        });
        showDialog("Orders by Branch", sb.toString());
    }

    private void showSalesByBranch() {
        StringBuilder sb = new StringBuilder();
        salesMap.forEach((branch, bs) ->
            sb.append(String.format("%-10s  →  KES %.2f%n", branch, bs.getTotalSales())));
        double total = salesMap.values().stream()
                               .mapToDouble(BranchSales::getTotalSales).sum();
        sb.append(String.format("%n%-10s  →  KES %.2f", "TOTAL", total));
        showDialog("Sales by Branch", sb.toString());
    }

    private void showStock() {
        StringBuilder sb = new StringBuilder();
        stockMap.forEach((branch, drinks) -> {
            sb.append("\n--- ").append(branch).append(" ---\n");
            drinks.forEach(d -> sb.append("  ").append(d).append("\n"));
        });
        showDialog("Current Stock Levels", sb.toString());
    }

    private void showDialog(String title, String content) {
        JTextArea ta = new JTextArea(content);
        ta.setEditable(false);
        ta.setFont(new Font("Monospaced", Font.PLAIN, 13));
        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(500, 350));
        JOptionPane.showMessageDialog(this, sp, title, JOptionPane.INFORMATION_MESSAGE);
    }

    // ── Init stock ────────────────────────────────────────
    private void initStock() {
        String[] branches = {"NAKURU", "MOMBASA", "KISUMU", "NAIROBI"};
        String[][] drinks = {
            {"Coca-Cola","Coca-Cola Co","50","20"},
            {"Pepsi","PepsiCo","45","20"},
            {"Fanta","Coca-Cola Co","45","20"},
            {"Sprite","Coca-Cola Co","45","20"},
            {"Water","Dasani","30","30"}
        };
        for (String b : branches) {
            salesMap.put(b, new BranchSales(b));
            List<Drink> list = new ArrayList<>();
            for (String[] d : drinks)
                list.add(new Drink(d[0], d[1],
                         Double.parseDouble(d[2]),
                         Integer.parseInt(d[3])));
            stockMap.put(b, list);
        }
    }

    // ── Main ──────────────────────────────────────────────
    public static void main(String[] args) {
        SwingUtilities.invokeLater(HeadquarterGUI::new);
    }
}
