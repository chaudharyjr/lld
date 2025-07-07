import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

// --- Basic Logging Utility (replace with SLF4J/Log4j in a real app) ---
class Logger {
    public static void info(String message) {
        System.out.println("[INFO] " + message);
    }

    public static void warn(String message) {
        System.out.println("[WARN] " + message);
    }

    public static void error(String message) {
        System.err.println("[ERROR] " + message);
    }
}

// --- Custom Exceptions ---
class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(String message) {
        super(message);
    }
}

class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) {
        super(message);
    }
}

class WarehouseNotFoundException extends RuntimeException {
    public WarehouseNotFoundException(String message) {
        super(message);
    }
}

// === PRODUCT ===
public abstract class Product {
    private final String id;
    private String name;
    private double price;
    private final AtomicInteger quantity; // Thread-safe quantity
    private int threshold;
    // No explicit lock needed here as AtomicInteger handles internal synchronization for quantity.
    // Other fields (id, name, price, threshold) are effectively immutable or rarely change.

    public Product(String id, String name, double price, int quantity, int threshold) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Product ID cannot be null or empty.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be null or empty.");
        }
        if (price <= 0) {
            throw new IllegalArgumentException("Product price must be positive.");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("Product quantity cannot be negative.");
        }
        if (threshold < 0) {
            throw new IllegalArgumentException("Product threshold cannot be negative.");
        }

        this.id = id;
        this.name = name;
        this.price = price;
        this.quantity = new AtomicInteger(quantity);
        this.threshold = threshold;
    }

    // Copy constructor for safer object transfers/cloning
    public Product(Product other, int newQuantity) {
        this.id = other.id;
        this.name = other.name;
        this.price = other.price;
        this.quantity = new AtomicInteger(newQuantity); // Set the new quantity for the copy
        this.threshold = other.threshold;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity.get(); } // Use get() for AtomicInteger
    public int getThreshold() { return threshold; }

    // No 'synchronized' keyword on these methods because AtomicInteger handles atomicity
    public void setQuantity(int newQuantity) {
        if (newQuantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative.");
        }
        this.quantity.set(newQuantity); // Use set() for AtomicInteger
    }

    public void increaseQuantity(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount to increase cannot be negative.");
        }
        this.quantity.addAndGet(amount); // Atomically add amount
    }

    public void reduceQuantity(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount to reduce cannot be negative.");
        }
        // This check and reduction needs to be atomic to prevent over-selling.
        // `compareAndSet` or a loop with `get` and `compareAndSet` is ideal,
        // but `addAndGet` after a check works in conjunction with calling context's locks.
        int currentQty = quantity.get();
        if (currentQty < amount) {
            throw new InsufficientStockException("Attempted to reduce quantity by " + amount +
                                                 " but only " + currentQty + " available for " + this.name);
        }
        // This specific operation needs the surrounding lock (e.g., in Warehouse's processOrder)
        // to ensure that the get() and addAndGet() are atomic relative to other warehouse operations.
        // If Product itself was shared among *different* Warehouse threads without external control,
        // a loop with compareAndSet would be better here. But within a single Warehouse's methods,
        // the Warehouse's WriteLock handles the overall atomicity of operations on its products.
        this.quantity.addAndGet(-amount);
    }

    // Abstract method to get the specific product type string for the factory
    public abstract String getProductType();

    @Override
    public String toString() {
        return String.format("%s (%s) - Qty: %d", name, id, quantity.get());
    }
}

// === EXAMPLE CONCRETE PRODUCTS ===
public class ElectronicProduct extends Product {
    public ElectronicProduct(String id, String name, double price, int quantity, int threshold) {
        super(id, name, price, quantity, threshold);
    }

    public ElectronicProduct(ElectronicProduct other, int newQuantity) {
        super(other, newQuantity);
    }

    @Override
    public String getProductType() {
        return "electronic";
    }
}

public class BookProduct extends Product {
    private String author;

    public BookProduct(String id, String name, double price, int quantity, int threshold, String author) {
        super(id, name, price, quantity, threshold);
        if (author == null || author.trim().isEmpty()) {
            throw new IllegalArgumentException("Book author cannot be null or empty.");
        }
        this.author = author;
    }

    public BookProduct(BookProduct other, int newQuantity) {
        super(other, newQuantity);
        this.author = other.author;
    }

    public String getAuthor() {
        return author;
    }

    @Override
    public String getProductType() {
        return "book";
    }

    @Override
    public String toString() {
        return String.format("%s by %s (%s) - Qty: %d", getName(), author, getId(), getQuantity());
    }
}


// === PRODUCT FACTORY ===
public class ProductFactory {
    public static Product createProduct(String type, String id, String name, double price, int quantity, int threshold, String... additionalArgs) {
        switch (type.toLowerCase()) {
            case "electronic":
                return new ElectronicProduct(id, name, price, quantity, threshold);
            case "book":
                if (additionalArgs.length < 1) {
                    throw new IllegalArgumentException("Book product requires an author.");
                }
                return new BookProduct(id, name, price, quantity, threshold, additionalArgs[0]);
            default:
                throw new IllegalArgumentException("Unknown product type: " + type);
        }
    }

    // Overloaded method to create a copy of an existing product for transfer
    public static Product createProductCopy(Product sourceProduct, int quantityForCopy) {
        if (sourceProduct instanceof ElectronicProduct) {
            return new ElectronicProduct((ElectronicProduct) sourceProduct, quantityForCopy);
        } else if (sourceProduct instanceof BookProduct) {
            return new BookProduct((BookProduct) sourceProduct, quantityForCopy);
        }
        // Add more product types here as they are introduced
        else {
            throw new IllegalArgumentException("Unsupported product type for copying: " + sourceProduct.getClass().getName());
        }
    }
}

// === OBSERVER PATTERN ===
public interface InventoryObserver {
    void notifyLowStock(Product product, Warehouse warehouse);
}

// === CONCRETE OBSERVER ===
public class EmailNotifier implements InventoryObserver {
    @Override
    public void notifyLowStock(Product product, Warehouse warehouse) {
        Logger.warn("[ALERT] Low stock for product " + product.getName() +
                " in warehouse " + warehouse.getName() + ". Quantity: " + product.getQuantity() +
                ". Threshold: " + product.getThreshold());
    }
}

// === WAREHOUSE ===
public class Warehouse {
    private final String id;
    private String name;
    private String location;

    // Use ReentrantReadWriteLock for fine-grained control over product map access
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();

    // No longer needs ConcurrentHashMap because ReentrantReadWriteLock handles map synchronization
    private final Map<String, Product> products;
    private final List<InventoryObserver> observers; // SynchronizedList needs explicit sync block for iteration

    public Warehouse(String id, String name, String location) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Warehouse ID cannot be null or empty.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Warehouse name cannot be null or empty.");
        }
        this.id = id;
        this.name = name;
        this.location = location;
        this.products = new HashMap<>(); // Using HashMap now, as rwLock protects it
        this.observers = Collections.synchronizedList(new ArrayList<>());
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getLocation() { return location; }

    public void addObserver(InventoryObserver observer) {
        observers.add(observer);
        Logger.info("Added observer to " + name + ": " + observer.getClass().getSimpleName());
    }

    public void removeObserver(InventoryObserver observer) {
        observers.remove(observer);
        Logger.info("Removed observer from " + name + ": " + observer.getClass().getSimpleName());
    }

    private void notifyObservers(Product product) {
        // Synchronize on the observers list itself for safe iteration
        synchronized (observers) {
            for (InventoryObserver observer : observers) {
                observer.notifyLowStock(product, this);
            }
        }
    }

    public void addProduct(Product product) {
        writeLock.lock(); // Acquire write lock
        try {
            if (product == null) {
                throw new IllegalArgumentException("Product cannot be null.");
            }
            if (products.containsKey(product.getId())) {
                Product existingProduct = products.get(product.getId());
                existingProduct.increaseQuantity(product.getQuantity()); // AtomicInteger handles its own quantity sync
                Logger.info("Product " + product.getName() + " already exists in " + name + ". Increased quantity by " + product.getQuantity() + ". New Qty: " + existingProduct.getQuantity());
            } else {
                products.put(product.getId(), product);
                Logger.info("Product added to warehouse " + name + ": " + product);
            }
        } finally {
            writeLock.unlock(); // Always release the lock
        }
    }

    public void removeProduct(String productId) {
        writeLock.lock();
        try {
            if (productId == null || productId.trim().isEmpty()) {
                throw new IllegalArgumentException("Product ID cannot be null or empty.");
            }
            Product removedProduct = products.remove(productId);
            if (removedProduct != null) {
                Logger.info("Product removed from warehouse " + name + ": " + removedProduct.getName());
            } else {
                Logger.warn("Attempted to remove non-existent product " + productId + " from warehouse " + name);
            }
        } finally {
            writeLock.unlock();
        }
    }

    public void updateProductQuantity(String productId, int newQuantity) {
        writeLock.lock();
        try {
            Product p = products.get(productId);
            if (p == null) {
                throw new ProductNotFoundException("Product with ID " + productId + " not found in warehouse " + name);
            }
            int oldQuantity = p.getQuantity();
            p.setQuantity(newQuantity); // AtomicInteger handles its own quantity sync
            Logger.info("Updated quantity of " + p.getName() + " in " + name + " from " + oldQuantity + " to " + newQuantity);
            if (newQuantity < p.getThreshold()) {
                notifyObservers(p);
            }
        } finally {
            writeLock.unlock();
        }
    }

    public Product getProduct(String productId) {
        readLock.lock(); // Acquire read lock
        try {
            if (productId == null || productId.trim().isEmpty()) {
                throw new IllegalArgumentException("Product ID cannot be null or empty.");
            }
            return products.get(productId);
        } finally {
            readLock.unlock(); // Always release the lock
        }
    }

    public Collection<Product> getAllProducts() {
        readLock.lock(); // Acquire read lock
        try {
            // Return a defensive copy to prevent external modification of the internal map
            return new ArrayList<>(products.values());
        } finally {
            readLock.unlock(); // Always release the lock
        }
    }

    public void processOrder(String productId, int quantity) {
        writeLock.lock(); // Acquire write lock as this modifies product quantity
        try {
            if (quantity <= 0) {
                throw new IllegalArgumentException("Order quantity must be positive.");
            }
            Product p = products.get(productId);
            if (p == null) {
                throw new ProductNotFoundException("Product with ID " + productId + " not found in warehouse " + name);
            }
            p.reduceQuantity(quantity); // AtomicInteger ensures quantity update is safe
            Logger.info("Order processed for " + p.getName() + ". Reduced quantity by " + quantity + ". Remaining quantity: " + p.getQuantity());
            if (p.getQuantity() < p.getThreshold()) {
                notifyObservers(p);
            }
        } catch (InsufficientStockException e) {
            Logger.error("Failed to process order for " + productId + ": " + e.getMessage());
            throw e;
        } finally {
            writeLock.unlock();
        }
    }

    public void transferProduct(String productId, int quantity, Warehouse targetWarehouse) {
        // Need to acquire locks on both source and target warehouses to prevent deadlocks.
        // A simple approach is to acquire locks in a consistent order (e.g., based on ID).
        // For simplicity, we'll acquire source writeLock first, then target writeLock.
        // For real-world, a more sophisticated global lock ordering or a transactional approach might be needed.

        // First, validate inputs
        if (quantity <= 0) {
            throw new IllegalArgumentException("Transfer quantity must be positive.");
        }
        if (targetWarehouse == null) {
            throw new IllegalArgumentException("Target warehouse cannot be null.");
        }
        if (this.equals(targetWarehouse)) {
            Logger.warn("Attempted to transfer product to the same warehouse: " + this.name);
            return;
        }

        // Acquire source warehouse's write lock
        this.writeLock.lock();
        try {
            Product sourceProduct = products.get(productId);
            if (sourceProduct == null) {
                throw new ProductNotFoundException("Product with ID " + productId + " not found in source warehouse " + this.name);
            }

            // Reduce quantity in source warehouse
            sourceProduct.reduceQuantity(quantity);
            Logger.info("Reduced " + quantity + " of " + sourceProduct.getName() + " from " + this.name + ". Remaining: " + sourceProduct.getQuantity());

            // Create a new product instance for the target warehouse using the copy constructor
            Product transferredProduct = ProductFactory.createProductCopy(sourceProduct, quantity);

            // Acquire target warehouse's write lock
            targetWarehouse.writeLock.lock();
            try {
                // Add/update in target warehouse
                // Note: targetWarehouse.addProduct() itself uses its own writeLock,
                // so nesting locks is critical. Always acquire outer lock first.
                // In a highly concurrent system, this could lead to contention/deadlock if not managed.
                // A better approach might be a global transfer service or a 2-phase commit like pattern.
                targetWarehouse.addProduct(transferredProduct);

                Logger.info("Transferred " + quantity + " of " + sourceProduct.getName() +
                        " from " + this.name + " to " + targetWarehouse.getName());

                // Check for low stock in source warehouse after transfer
                if (sourceProduct.getQuantity() < sourceProduct.getThreshold()) {
                    notifyObservers(sourceProduct);
                }
            } finally {
                targetWarehouse.writeLock.unlock(); // Release target lock
            }
        } catch (InsufficientStockException e) {
            Logger.error("Cannot transfer product " + productId + ": " + e.getMessage());
            throw e;
        } catch (ProductNotFoundException e) {
            Logger.error("Error during product transfer: " + e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            Logger.error("Error during product transfer: " + e.getMessage());
            throw e;
        } finally {
            this.writeLock.unlock(); // Always release source lock
        }
    }
}

// === STRATEGY PATTERN ===
public interface ReplenishmentStrategy {
    void replenish(Product product);
}

public class BulkReplenishmentStrategy implements ReplenishmentStrategy {
    @Override
    public void replenish(Product product) {
        int replenishAmount = 100;
        product.increaseQuantity(replenishAmount);
        Logger.info("Bulk replenishment for " + product.getName() + ". Added " + replenishAmount + " units.");
    }
}

public class JustInTimeReplenishmentStrategy implements ReplenishmentStrategy {
    @Override
    public void replenish(Product product) {
        int targetQuantity = product.getThreshold() + 10;
        int currentQuantity = product.getQuantity();
        if (currentQuantity < targetQuantity) {
            int amountToAdd = targetQuantity - currentQuantity;
            product.increaseQuantity(amountToAdd);
            Logger.info("Just-in-Time replenishment for " + product.getName() + ". Added " + amountToAdd + " units to reach " + targetQuantity + ".");
        } else {
            Logger.info("No JIT replenishment needed for " + product.getName() + ". Current quantity " + currentQuantity + " is already at or above target " + targetQuantity + ".");
        }
    }
}

// === SINGLETON INVENTORY MANAGER ===
public class InventoryManager {
    private static InventoryManager instance;

    // Use ReentrantReadWriteLock for fine-grained control over warehouse map access
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();

    private final Map<String, Warehouse> warehouses; // HashMap now, protected by rwLock

    private InventoryManager() {
        warehouses = new HashMap<>(); // Using HashMap now, as rwLock protects it
    }

    // Still synchronized for the Singleton instance creation
    public static synchronized InventoryManager getInstance() {
        if (instance == null) {
            instance = new InventoryManager();
        }
        return instance;
    }

    public void addWarehouse(Warehouse warehouse) {
        writeLock.lock(); // Acquire write lock
        try {
            if (warehouse == null) {
                throw new IllegalArgumentException("Warehouse cannot be null.");
            }
            if (warehouses.containsKey(warehouse.getId())) {
                Logger.warn("Warehouse with ID " + warehouse.getId() + " already exists. Not adding duplicate.");
                return;
            }
            warehouses.put(warehouse.getId(), warehouse);
            Logger.info("Warehouse added: " + warehouse.getName());
        } finally {
            writeLock.unlock(); // Always release lock
        }
    }

    public void removeWarehouse(String warehouseId) {
        writeLock.lock(); // Acquire write lock
        try {
            if (warehouseId == null || warehouseId.trim().isEmpty()) {
                throw new IllegalArgumentException("Warehouse ID cannot be null or empty.");
            }
            Warehouse removed = warehouses.remove(warehouseId);
            if (removed != null) {
                Logger.info("Warehouse removed: " + removed.getName());
            } else {
                Logger.warn("Attempted to remove non-existent warehouse with ID: " + warehouseId);
            }
        } finally {
            writeLock.unlock(); // Always release lock
        }
    }

    public Warehouse getWarehouse(String id) {
        readLock.lock(); // Acquire read lock
        try {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("Warehouse ID cannot be null or empty.");
            }
            return warehouses.get(id);
        } finally {
            readLock.unlock(); // Always release lock
        }
    }

    public void replenishStock(String warehouseId, String productId, ReplenishmentStrategy strategy) {
        // Note: Replenishment involves reading the warehouse map (readLock)
        // and then potentially modifying a product within that warehouse (which uses warehouse's writeLock).
        // This pattern needs careful consideration for deadlocks if not handled with a global manager.
        // For simplicity, we'll acquire readLock for manager's map first.
        // The actual product modification is handled by the Warehouse's internal locking.
        readLock.lock();
        try {
            if (warehouseId == null || warehouseId.trim().isEmpty()) {
                throw new IllegalArgumentException("Warehouse ID cannot be null or empty.");
            }
            if (productId == null || productId.trim().isEmpty()) {
                throw new IllegalArgumentException("Product ID cannot be null or empty.");
            }
            if (strategy == null) {
                throw new IllegalArgumentException("Replenishment strategy cannot be null.");
            }

            Warehouse warehouse = warehouses.get(warehouseId);
            if (warehouse == null) {
                throw new WarehouseNotFoundException("Warehouse with ID " + warehouseId + " not found.");
            }
            Product product = warehouse.getProduct(productId); // This acquires warehouse's read lock
            if (product == null) {
                throw new ProductNotFoundException("Product with ID " + productId + " not found in warehouse " + warehouse.getName());
            }
            strategy.replenish(product); // This modifies product using its AtomicInteger
            Logger.info("Replenished stock for " + product.getName() + " in " + warehouse.getName() + ". New quantity: " + product.getQuantity());
        } finally {
            readLock.unlock();
        }
    }
}

// === MAIN DEMO ===
public class Main {
    public static void main(String[] args) {
        Logger.info("Starting Inventory Management System Demo...");
        InventoryManager manager = InventoryManager.getInstance();

        // --- Setup Warehouses ---
        Logger.info("\n--- Setting up Warehouses ---");
        Warehouse warehouse1 = new Warehouse("W1", "Main Warehouse", "Bengaluru, India");
        warehouse1.addObserver(new EmailNotifier());
        manager.addWarehouse(warehouse1);

        Warehouse warehouse2 = new Warehouse("W2", "Distribution Hub", "Mumbai, India");
        warehouse2.addObserver(new EmailNotifier());
        manager.addWarehouse(warehouse2);

        // --- Create and Add Products ---
        Logger.info("\n--- Creating and Adding Products ---");
        try {
            Product laptop = ProductFactory.createProduct("electronic", "P1", "Laptop Pro", 1200.0, 5, 3);
            Product phone = ProductFactory.createProduct("electronic", "P2", "Smartphone X", 700.0, 10, 5);
            Product historyBook = ProductFactory.createProduct("book", "B1", "A Brief History of Time", 15.0, 20, 8, "Stephen Hawking");
            Product novel = ProductFactory.createProduct("book", "B2", "1984", 10.0, 12, 5, "George Orwell");

            warehouse1.addProduct(laptop);
            warehouse1.addProduct(phone);
            warehouse1.addProduct(historyBook);
            warehouse2.addProduct(novel);
            warehouse2.addProduct(ProductFactory.createProduct("electronic", "P3", "Smartwatch", 250.0, 7, 2));

            Logger.info("\n--- Initial Inventory Snapshot ---");
            System.out.println("Warehouse 1 Products: " + warehouse1.getAllProducts());
            System.out.println("Warehouse 2 Products: " + warehouse2.getAllProducts());

            // --- Process an Order ---
            Logger.info("\n--- Processing Order for P1 (Laptop Pro) in W1 ---");
            try {
                warehouse1.processOrder("P1", 3); // Reduces P1 quantity from 5 to 2 (below threshold 3)
            } catch (InsufficientStockException e) {
                Logger.error(e.getMessage());
            }

            // --- Replenish Stock using Strategy ---
            Logger.info("\n--- Replenishing P1 (Laptop Pro) in W1 using JIT Strategy ---");
            manager.replenishStock("W1", "P1", new JustInTimeReplenishmentStrategy()); // Qty becomes 3+10 = 13 (threshold + 10)

            Logger.info("\n--- Replenishing B1 (History Book) in W1 using Bulk Strategy ---");
            manager.replenishStock("W1", "B1", new BulkReplenishmentStrategy()); // Qty becomes 20+100 = 120

            // --- Attempt to process order that would cause insufficient stock ---
            Logger.info("\n--- Attempting to process large order for P2 (Smartphone X) in W1 ---");
            try {
                warehouse1.processOrder("P2", 20); // Qty is 10, requests 20
            } catch (InsufficientStockException e) {
                Logger.error(e.getMessage());
            }

            // --- Transfer Product Between Warehouses ---
            Logger.info("\n--- Transferring P2 (Smartphone X) from W1 to W2 ---");
            try {
                warehouse1.transferProduct("P2", 4, warehouse2); // W1 P2: 10 -> 6. W2 P2: (add 4)
            } catch (InsufficientStockException | ProductNotFoundException e) {
                Logger.error(e.getMessage());
            }

            Logger.info("\n--- Transferring B1 (History Book) from W1 to W2 ---");
            try {
                warehouse1.transferProduct("B1", 5, warehouse2); // W1 B1: 120 -> 115. W2 B1: (add 5, assuming no B1 in W2 initially)
            } catch (InsufficientStockException | ProductNotFoundException e) {
                Logger.error(e.getMessage());
            }

            Logger.info("\n--- Final Inventory Snapshot ---");
            System.out.println("Warehouse 1 Products: " + warehouse1.getAllProducts());
            System.out.println("Warehouse 2 Products: " + warehouse2.getAllProducts());

            // --- Demonstrate adding a product that already exists in a warehouse ---
            Logger.info("\n--- Adding existing product P3 to Warehouse 2 (should increase quantity) ---");
            Product anotherSmartwatch = ProductFactory.createProduct("electronic", "P3", "Smartwatch", 250.0, 3, 2);
            warehouse2.addProduct(anotherSmartwatch);
            System.out.println("Warehouse 2 Products after adding P3 again: " + warehouse2.getAllProducts());

            // --- Demonstrate invalid operations ---
            Logger.info("\n--- Demonstrating invalid operations ---");
            try {
                warehouse1.processOrder("NON_EXISTENT_PRODUCT", 1);
            } catch (ProductNotFoundException e) {
                Logger.error(e.getMessage());
            }
            try {
                manager.replenishStock("NON_EXISTENT_WAREHOUSE", "P1", new BulkReplenishmentStrategy());
            } catch (WarehouseNotFoundException e) {
                Logger.error(e.getMessage());
            }
            try {
                Product invalidProduct = ProductFactory.createProduct("unknown_type", "XX", "Test", 10.0, 5, 1);
            } catch (IllegalArgumentException e) {
                Logger.error(e.getMessage());
            }

        } catch (IllegalArgumentException | ProductNotFoundException | InsufficientStockException | WarehouseNotFoundException e) {
            Logger.error("An unexpected error occurred during demo execution: " + e.getMessage());
        }
    }
}
```
