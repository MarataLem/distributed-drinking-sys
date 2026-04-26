package common;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Order implements Serializable {
    private static final long serialVersionUID = 1L;
    private String customerName;
    private String drinkName;
    private String branch;          // branch where order was placed
    private int    quantity;
    private double totalAmount;
    private LocalDateTime timestamp;

    public Order(String customerName, String drinkName, String branch,
                 int quantity, double totalAmount) {
        this.customerName = customerName;
        this.drinkName    = drinkName;
        this.branch       = branch;
        this.quantity     = quantity;
        this.totalAmount  = totalAmount;
        this.timestamp    = LocalDateTime.now();
    }

    public String getCustomerName() { return customerName; }
    public String getDrinkName()    { return drinkName; }
    public String getBranch()       { return branch; }
    public int    getQuantity()     { return quantity; }
    public double getTotalAmount()  { return totalAmount; }
    public LocalDateTime getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return "[" + timestamp + "] Customer: " + customerName
             + " | Drink: " + drinkName + " x" + quantity
             + " | Branch: " + branch
             + " | Total: KES " + totalAmount;
    }
}
