package common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BranchSales implements Serializable {
    private static final long serialVersionUID = 1L;
    private String branchName;
    private List<Order> orders = new ArrayList<>();

    public BranchSales(String branchName) {
        this.branchName = branchName;
    }

    public void addOrder(Order o) { orders.add(o); }

    public String getBranchName() { return branchName; }
    public List<Order> getOrders() { return orders; }

    public double getTotalSales() {
        return orders.stream().mapToDouble(Order::getTotalAmount).sum();
    }
}
