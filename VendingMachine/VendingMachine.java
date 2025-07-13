import java.util.*;

// ----------------- ENUM: Denomination -----------------
enum Denomination {
    COIN_1(1), COIN_5(5), COIN_10(10), NOTE_20(20), NOTE_50(50), NOTE_100(100);
    private final int value;
    Denomination(int value) { this.value = value; }
    public int getValue() { return value; }
}

// ----------------- CLASS: Product -----------------
class Product {
    private final String id;
    private final String name;
    private final int price;

    public Product(String id, String name, int price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getPrice() { return price; }

    @Override
    public String toString() {
        return "[" + name + ": ₹" + price + "]";
    }
}

// ----------------- GENERIC: Inventory<T> -----------------
class Inventory<T> {
    private final Map<T, Integer> items = new HashMap<>();

    public synchronized void add(T item, int qty) {
        items.put(item, items.getOrDefault(item, 0) + qty);
    }

    public synchronized boolean deduct(T item, int qty) {
        int count = items.getOrDefault(item, 0);
        if (count < qty) return false;
        items.put(item, count - qty);
        return true;
    }

    public synchronized int getQuantity(T item) {
        return items.getOrDefault(item, 0);
    }

    public synchronized Map<T, Integer> getAll() {
        return new HashMap<>(items);
    }

    public synchronized void clear() {
        items.clear();
    }
}

// ----------------- STATE INTERFACE -----------------
interface VendingState {
    void insertMoney(int amount);
    void selectProduct(String productId);
    void dispense();
    void refund();
}

// ----------------- CONTEXT: VendingMachine -----------------
class VendingMachine {
    private VendingState idleState;
    private VendingState hasMoneyState;
    private VendingState dispensingState;
    private VendingState outOfServiceState;

    private VendingState currentState;

    private int balance = 0;
    private Product selectedProduct;

    private final Inventory<Product> productInventory = new Inventory<>();
    private final Inventory<Denomination> cashInventory = new Inventory<>();

    // State instances
    public VendingMachine() {
        idleState = new IdleState(this);
        hasMoneyState = new HasMoneyState(this);
        dispensingState = new DispensingState(this);
        outOfServiceState = new OutOfServiceState(this);
        currentState = idleState;
    }

    // State transitions
    public void setState(VendingState state) { this.currentState = state; }
    public VendingState getIdleState() { return idleState; }
    public VendingState getHasMoneyState() { return hasMoneyState; }
    public VendingState getDispensingState() { return dispensingState; }
    public VendingState getOutOfServiceState() { return outOfServiceState; }

    // Operations delegated to state
    public void insertMoney(int amount) { currentState.insertMoney(amount); }
    public void selectProduct(String id) { currentState.selectProduct(id); }
    public void dispense() { currentState.dispense(); }
    public void refund() { currentState.refund(); }

    // Balance handling
    public int getBalance() { return balance; }
    public void addBalance(int amount) { balance += amount; }
    public void resetBalance() { balance = 0; }

    // Selected product
    public Product getSelectedProduct() { return selectedProduct; }
    public void setSelectedProduct(Product product) { this.selectedProduct = product; }

    // Inventories
    public Inventory<Product> getProductInventory() { return productInventory; }
    public Inventory<Denomination> getCashInventory() { return cashInventory; }

    // Admin ops
    public void restockProduct(Product p, int qty) {
        productInventory.add(p, qty);
        System.out.println("Admin restocked: " + p.getName() + " x" + qty);
    }

    public void loadCash(Denomination denom, int qty) {
        cashInventory.add(denom, qty);
        System.out.println("Admin loaded: " + denom + " x" + qty);
    }

    public void showStatus() {
        System.out.println("==== Machine Status ====");
        System.out.println("Current Balance: ₹" + balance);
        System.out.println("Products:");
        for (var e : productInventory.getAll().entrySet()) {
            System.out.println("  " + e.getKey() + " | Qty: " + e.getValue());
        }
        System.out.println("Cash Inventory:");
        for (var e : cashInventory.getAll().entrySet()) {
            System.out.println("  " + e.getKey() + " | Qty: " + e.getValue());
        }
        System.out.println("========================");
    }
}

// ----------------- STATE: IdleState -----------------
class IdleState implements VendingState {
    private final VendingMachine machine;
    public IdleState(VendingMachine m) { this.machine = m; }

    @Override
    public void insertMoney(int amount) {
        System.out.println("Inserted ₹" + amount);
        machine.addBalance(amount);
        machine.setState(machine.getHasMoneyState());
    }

    @Override
    public void selectProduct(String id) {
        System.out.println("Insert money first before selecting product.");
    }

    @Override
    public void dispense() {
        System.out.println("No product selected.");
    }

    @Override
    public void refund() {
        System.out.println("No money to refund.");
    }
}

// ----------------- STATE: HasMoneyState -----------------
class HasMoneyState implements VendingState {
    private final VendingMachine machine;
    public HasMoneyState(VendingMachine m) { this.machine = m; }

    @Override
    public void insertMoney(int amount) {
        System.out.println("Added ₹" + amount);
        machine.addBalance(amount);
    }

    @Override
    public void selectProduct(String id) {
        Product selected = null;
        for (Product p : machine.getProductInventory().getAll().keySet()) {
            if (p.getId().equals(id)) {
                selected = p;
                break;
            }
        }
        if (selected == null) {
            System.out.println("Product not found.");
            return;
        }
        if (machine.getProductInventory().getQuantity(selected) <= 0) {
            System.out.println("Product is out of stock!");
            return;
        }
        if (machine.getBalance() < selected.getPrice()) {
            System.out.println("Insufficient balance. Price is ₹" + selected.getPrice());
            return;
        }

        System.out.println("Product selected: " + selected.getName());
        machine.setSelectedProduct(selected);
        machine.setState(machine.getDispensingState());
        machine.dispense();
    }

    @Override
    public void dispense() {
        System.out.println("Please select a product first.");
    }

    @Override
    public void refund() {
        System.out.println("Refunding ₹" + machine.getBalance());
        machine.resetBalance();
        machine.setState(machine.getIdleState());
    }
}

// ----------------- STATE: DispensingState -----------------
class DispensingState implements VendingState {
    private final VendingMachine machine;
    public DispensingState(VendingMachine m) { this.machine = m; }

    @Override
    public void insertMoney(int amount) {
        System.out.println("Dispensing in progress. Please wait.");
    }

    @Override
    public void selectProduct(String id) {
        System.out.println("Dispensing in progress. Please wait.");
    }

    @Override
    public void dispense() {
        Product p = machine.getSelectedProduct();
        machine.getProductInventory().deduct(p, 1);
        System.out.println("Dispensing: " + p.getName());

        int change = machine.getBalance() - p.getPrice();
        if (change > 0) {
            System.out.println("Returning change: ₹" + change);
        }

        machine.resetBalance();
        machine.setSelectedProduct(null);
        machine.setState(machine.getIdleState());
    }

    @Override
    public void refund() {
        System.out.println("Cannot refund while dispensing.");
    }
}

// ----------------- STATE: OutOfServiceState -----------------
class OutOfServiceState implements VendingState {
    private final VendingMachine machine;
    public OutOfServiceState(VendingMachine m) { this.machine = m; }

    @Override
    public void insertMoney(int amount) {
        System.out.println("Machine is out of service.");
    }

    @Override
    public void selectProduct(String id) {
        System.out.println("Machine is out of service.");
    }

    @Override
    public void dispense() {
        System.out.println("Machine is out of service.");
    }

    @Override
    public void refund() {
        System.out.println("Machine is out of service.");
    }
}

// ----------------- MAIN DEMO -----------------
public class VendingMachineSystem {
    public static void main(String[] args) {
        VendingMachine vm = new VendingMachine();

        // Admin setup
        Product chips = new Product("P1", "Chips", 15);
        Product soda = new Product("P2", "Soda", 25);
        vm.restockProduct(chips, 5);
        vm.restockProduct(soda, 3);
        vm.loadCash(Denomination.COIN_1, 50);
        vm.loadCash(Denomination.COIN_5, 20);
        vm.loadCash(Denomination.COIN_10, 10);

        vm.showStatus();

        // Customer interaction
        System.out.println("\n--- Customer 1 ---");
        vm.insertMoney(10);
        vm.insertMoney(10);
        vm.selectProduct("P2");  // Soda costs ₹25

        vm.showStatus();

        System.out.println("\n--- Customer 2 ---");
        vm.insertMoney(5);
        vm.selectProduct("P1");  // Chips costs ₹15
        vm.insertMoney(10);
        vm.selectProduct("P1");

        vm.showStatus();

        System.out.println("\n--- Customer 3 refunds ---");
        vm.insertMoney(20);
        vm.refund();

        vm.showStatus();
    }
}
