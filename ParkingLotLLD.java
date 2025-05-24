import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID; // For generating unique IDs
import java.util.concurrent.TimeUnit; // For time calculations

// --- Enums ---

/**
 * Represents different types of vehicles supported by the parking lot.
 */
enum VehicleType {
    CAR,
    BIKE,
    TRUCK,
    VAN,
    ELECTRIC_CAR
}

/**
 * Represents different types of parking spots available.
 */
enum ParkingSpotType {
    SMALL, // Suitable for Bikes
    MEDIUM, // Suitable for Cars, Vans
    LARGE, // Suitable for Trucks, Vans
    ELECTRIC_CHARGING // For electric vehicles
}

/**
 * Represents the type of a gate (entry or exit).
 */
enum GateType {
    ENTRY,
    EXIT
}

/**
 * Represents the status of a payment transaction.
 */
enum PaymentStatus {
    PENDING,
    COMPLETED,
    FAILED,
    REFUNDED
}

// --- Interfaces for Strategies ---

/**
 * Defines the contract for different pricing models (fee calculation strategies).
 */
interface PricingStrategy {
    /**
     * Calculates the parking fee based on entry/exit times and vehicle type.
     * @param entryTime Entry timestamp in milliseconds.
     * @param exitTime Exit timestamp in milliseconds.
     * @param vehicleType Type of the vehicle.
     * @return The calculated parking fee.
     */
    double calculatePrice(long entryTime, long exitTime, VehicleType vehicleType);
}

/**
 * Defines the contract for different payment methods.
 */
interface PaymentStrategy {
    /**
     * Executes the payment process for a given amount.
     * @param amount The amount to be paid.
     * @return true if payment is successful, false otherwise.
     */
    boolean pay(double amount);
}

// --- Concrete Pricing Strategies ---

/**
 * Implements an hourly pricing strategy.
 */
class HourlyPricing implements PricingStrategy {
    private Map<VehicleType, Double> hourlyRates;

    /**
     * Constructor for HourlyPricing.
     * @param hourlyRates A map of VehicleType to their respective hourly rates.
     */
    public HourlyPricing(Map<VehicleType, Double> hourlyRates) {
        this.hourlyRates = hourlyRates;
    }

    @Override
    public double calculatePrice(long entryTime, long exitTime, VehicleType vehicleType) {
        long durationMillis = exitTime - entryTime;
        // Ensure minimum 1 hour charge if duration is positive but less than an hour
        double hours = Math.max(1, Math.ceil((double) durationMillis / TimeUnit.HOURS.toMillis(1)));
        double rate = hourlyRates.getOrDefault(vehicleType, 0.0); // Default to 0 if rate not found
        return hours * rate;
    }
}

/**
 * Implements a daily pricing strategy.
 */
class DailyPricing implements PricingStrategy {
    private Map<VehicleType, Double> dailyRates;

    /**
     * Constructor for DailyPricing.
     * @param dailyRates A map of VehicleType to their respective daily rates.
     */
    public DailyPricing(Map<VehicleType, Double> dailyRates) {
        this.dailyRates = dailyRates;
    }

    @Override
    public double calculatePrice(long entryTime, long exitTime, VehicleType vehicleType) {
        long durationMillis = exitTime - entryTime;
        // Ensure minimum 1 day charge if duration is positive but less than a day
        double days = Math.max(1, Math.ceil((double) durationMillis / TimeUnit.DAYS.toMillis(1)));
        double rate = dailyRates.getOrDefault(vehicleType, 0.0); // Default to 0 if rate not found
        return days * rate;
    }
}

// --- Concrete Payment Strategies ---

/**
 * Implements payment via Cash.
 */
class CashPayment implements PaymentStrategy {
    @Override
    public boolean pay(double amount) {
        System.out.println("Processing cash payment of $" + String.format("%.2f", amount));
        // Simulate cash handling logic (e.g., check for exact change, return change)
        System.out.println("Payment successful via Cash.");
        return true;
    }
}

/**
 * Implements payment via Card.
 */
class CardPayment implements PaymentStrategy {
    @Override
    public boolean pay(double amount) {
        System.out.println("Processing card payment of $" + String.format("%.2f", amount));
        // Simulate interaction with a card processing gateway
        // For simplicity, we'll assume it always succeeds.
        System.out.println("Payment successful via Card.");
        return true;
    }
}

// --- Core Classes ---

/**
 * Represents a vehicle entering the parking lot.
 */
class Vehicle {
    private String licensePlate;
    private VehicleType type;
    private long entryTime; // Timestamp when the vehicle entered (milliseconds since epoch)

    /**
     * Constructor for Vehicle.
     * @param licensePlate Unique identifier for the vehicle.
     * @param type The type of vehicle (e.g., CAR, TRUCK).
     */
    public Vehicle(String licensePlate, VehicleType type) {
        this.licensePlate = licensePlate;
        this.type = type;
    }

    /**
     * Returns the vehicle type.
     * @return The vehicle type.
     */
    public VehicleType getType() {
        return type;
    }

    /**
     * Returns the license plate.
     * @return The license plate.
     */
    public String getLicensePlate() {
        return licensePlate;
    }

    /**
     * Sets the entry time for the vehicle.
     * @param entryTime The entry timestamp in milliseconds.
     */
    public void setEntryTime(long entryTime) {
        this.entryTime = entryTime;
    }

    /**
     * Returns the entry time of the vehicle.
     * @return The entry timestamp in milliseconds.
     */
    public long getEntryTime() {
        return entryTime;
    }

    @Override
    public String toString() {
        return "Vehicle [licensePlate=" + licensePlate + ", type=" + type + "]";
    }
}

/**
 * Represents a single parking spot within a floor.
 */
class ParkingSpot {
    private String spotId;
    private ParkingSpotType type;
    private boolean isOccupied;
    private Vehicle vehicle; // Reference to the Vehicle currently occupying the spot (null if empty)

    /**
     * Constructor for ParkingSpot.
     * @param spotId Unique identifier for the spot (e.g., "F1-S001").
     * @param type The type of spot (e.g., SMALL, LARGE).
     */
    public ParkingSpot(String spotId, ParkingSpotType type) {
        this.spotId = spotId;
        this.type = type;
        this.isOccupied = false;
        this.vehicle = null;
    }

    /**
     * Assigns a vehicle to this spot.
     * @param vehicle The vehicle to assign.
     * @return true on success, false if already occupied.
     */
    public boolean assignVehicle(Vehicle vehicle) {
        if (!isOccupied) {
            this.vehicle = vehicle;
            this.isOccupied = true;
            System.out.println("Assigned " + vehicle.getLicensePlate() + " to spot " + spotId);
            return true;
        }
        System.out.println("Spot " + spotId + " is already occupied.");
        return false;
    }

    /**
     * Removes the vehicle from this spot.
     * @return true on success, false if already empty.
     */
    public boolean removeVehicle() {
        if (isOccupied) {
            System.out.println("Removed " + this.vehicle.getLicensePlate() + " from spot " + spotId);
            this.vehicle = null;
            this.isOccupied = false;
            return true;
        }
        System.out.println("Spot " + spotId + " is already empty.");
        return false;
    }

    /**
     * Checks if the spot is available.
     * @return true if available, false otherwise.
     */
    public boolean isAvailable() {
        return !isOccupied;
    }

    /**
     * Returns the spot type.
     * @return The ParkingSpotType.
     */
    public ParkingSpotType getType() {
        return type;
    }

    /**
     * Returns the spot ID.
     * @return The spot ID.
     */
    public String getSpotId() {
        return spotId;
    }

    /**
     * Returns the vehicle occupying the spot.
     * @return The Vehicle object, or null if empty.
     */
    public Vehicle getVehicle() {
        return vehicle;
    }

    @Override
    public String toString() {
        return "ParkingSpot [spotId=" + spotId + ", type=" + type + ", isOccupied=" + isOccupied + "]";
    }
}

/**
 * Manages a collection of parking spots on a single floor.
 */
class ParkingFloor {
    private int floorNumber;
    private List<ParkingSpot> spots;
    private Map<ParkingSpotType, Integer> availableSpots; // Tracks count of available spots by type

    /**
     * Constructor for ParkingFloor.
     * @param floorNumber The unique number of the floor.
     */
    public ParkingFloor(int floorNumber) {
        this.floorNumber = floorNumber;
        this.spots = new ArrayList<>();
        this.availableSpots = new HashMap<>();
        // Initialize available spots count for all types to 0
        for (ParkingSpotType type : ParkingSpotType.values()) {
            availableSpots.put(type, 0);
        }
    }

    /**
     * Adds a parking spot to the floor. Updates availableSpots count.
     * @param spot The ParkingSpot to add.
     */
    public void addSpot(ParkingSpot spot) {
        spots.add(spot);
        if (spot.isAvailable()) {
            availableSpots.put(spot.getType(), availableSpots.get(spot.getType()) + 1);
        }
    }

    /**
     * Finds an available spot suitable for the given vehicle type.
     * Prioritizes exact matches, then larger spots if necessary.
     * @param vehicleType The type of vehicle looking for a spot.
     * @return An available ParkingSpot, or null if no suitable spot found.
     */
    public ParkingSpot findAvailableSpot(VehicleType vehicleType) {
        // Define suitable spot types based on vehicle type hierarchy
        List<ParkingSpotType> suitableSpotTypes = new ArrayList<>();
        switch (vehicleType) {
            case BIKE:
                suitableSpotTypes.add(ParkingSpotType.SMALL);
                suitableSpotTypes.add(ParkingSpotType.MEDIUM);
                suitableSpotTypes.add(ParkingSpotType.LARGE);
                break;
            case CAR:
            case VAN:
                suitableSpotTypes.add(ParkingSpotType.MEDIUM);
                suitableSpotTypes.add(ParkingSpotType.LARGE);
                break;
            case TRUCK:
                suitableSpotTypes.add(ParkingSpotType.LARGE);
                break;
            case ELECTRIC_CAR:
                suitableSpotTypes.add(ParkingSpotType.ELECTRIC_CHARGING);
                suitableSpotTypes.add(ParkingSpotType.MEDIUM); // Can also fit in regular medium spots
                suitableSpotTypes.add(ParkingSpotType.LARGE);  // Can also fit in regular large spots
                break;
        }

        // Iterate through suitable spot types in order of preference
        for (ParkingSpotType spotType : suitableSpotTypes) {
            if (availableSpots.getOrDefault(spotType, 0) > 0) { // Check if there are any available spots of this type
                for (ParkingSpot spot : spots) {
                    if (spot.isAvailable() && spot.getType() == spotType) {
                        return spot; // Found an available spot of the desired type
                    }
                }
            }
        }
        return null; // No suitable spot found
    }

    /**
     * Parks a vehicle in the given spot. Updates availableSpots count.
     * @param vehicle The vehicle to park.
     * @param spot The ParkingSpot where the vehicle will be parked.
     * @return true if parking was successful, false otherwise.
     */
    public boolean parkVehicle(Vehicle vehicle, ParkingSpot spot) {
        if (spot.assignVehicle(vehicle)) {
            availableSpots.put(spot.getType(), availableSpots.get(spot.getType()) - 1);
            System.out.println("Vehicle " + vehicle.getLicensePlate() + " parked on Floor " + floorNumber + " at " + spot.getSpotId());
            return true;
        }
        return false;
    }

    /**
     * Unparks a vehicle from the given spot. Updates availableSpots count.
     * @param spot The ParkingSpot from which the vehicle will be unparked.
     * @return true if unparking was successful, false otherwise.
     */
    public boolean unparkVehicle(ParkingSpot spot) {
        if (spot.removeVehicle()) {
            availableSpots.put(spot.getType(), availableSpots.get(spot.getType()) + 1);
            System.out.println("Vehicle unparked from Floor " + floorNumber + " at " + spot.getSpotId());
            return true;
        }
        return false;
    }

    /**
     * Returns the number of available spots for a specific type on this floor.
     * @param type The ParkingSpotType to check.
     * @return The count of available spots.
     */
    public int getAvailableSpotsCount(ParkingSpotType type) {
        return availableSpots.getOrDefault(type, 0);
    }

    /**
     * Returns the floor number.
     * @return The floor number.
     */
    public int getFloorNumber() {
        return floorNumber;
    }

    /**
     * Gets all spots on this floor.
     * @return A list of ParkingSpot objects.
     */
    public List<ParkingSpot> getSpots() {
        return spots;
    }
}

/**
 * Represents a parking ticket issued upon entry.
 */
class Ticket {
    private String ticketId;
    private Vehicle vehicle;
    private long entryTime;
    private ParkingSpot spot;

    /**
     * Constructor for Ticket.
     * @param ticketId Unique ID for the ticket.
     * @param vehicle Reference to the parked vehicle.
     * @param entryTime Timestamp of entry.
     * @param spot Reference to the assigned parking spot.
     */
    public Ticket(String ticketId, Vehicle vehicle, long entryTime, ParkingSpot spot) {
        this.ticketId = ticketId;
        this.vehicle = vehicle;
        this.entryTime = entryTime;
        this.spot = spot;
    }

    /**
     * Returns the ticket ID.
     * @return The ticket ID.
     */
    public String getTicketId() {
        return ticketId;
    }

    /**
     * Returns the associated vehicle.
     * @return The Vehicle object.
     */
    public Vehicle getVehicle() {
        return vehicle;
    }

    /**
     * Returns the entry time.
     * @return The entry timestamp in milliseconds.
     */
    public long getEntryTime() {
        return entryTime;
    }

    /**
     * Returns the assigned parking spot.
     * @return The ParkingSpot object.
     */
    public ParkingSpot getSpot() {
        return spot;
    }

    @Override
    public String toString() {
        return "Ticket [ticketId=" + ticketId + ", vehicle=" + vehicle.getLicensePlate() + ", entryTime=" + entryTime + ", spot=" + spot.getSpotId() + "]";
    }
}

/**
 * Represents a payment transaction.
 */
class Payment {
    private String transactionId;
    private Ticket ticket;
    private double amount;
    private PaymentStatus status;
    private long paymentTime;
    private PaymentStrategy paymentStrategy; // Injected payment strategy

    /**
     * Constructor for Payment.
     * @param transactionId Unique ID for the payment transaction.
     * @param ticket Reference to the ticket for which payment is made.
     * @param amount The amount to be paid.
     * @param paymentStrategy The strategy to use for processing this payment.
     */
    public Payment(String transactionId, Ticket ticket, double amount, PaymentStrategy paymentStrategy) {
        this.transactionId = transactionId;
        this.ticket = ticket;
        this.amount = amount;
        this.status = PaymentStatus.PENDING; // Initial status
        this.paymentTime = 0; // Will be set upon successful processing
        this.paymentStrategy = paymentStrategy;
    }

    /**
     * Processes the payment using the injected PaymentStrategy.
     * @return true on success, false on failure.
     */
    public boolean processPayment() {
        System.out.println("Initiating payment for Ticket ID: " + ticket.getTicketId() + ", Amount: $" + String.format("%.2f", amount));
        if (paymentStrategy.pay(amount)) {
            this.status = PaymentStatus.COMPLETED;
            this.paymentTime = System.currentTimeMillis();
            System.out.println("Payment successful. Transaction ID: " + transactionId);
            return true;
        } else {
            this.status = PaymentStatus.FAILED;
            System.out.println("Payment failed for Ticket ID: " + ticket.getTicketId());
            return false;
        }
    }

    /**
     * Returns the payment status.
     * @return The PaymentStatus.
     */
    public PaymentStatus getStatus() {
        return status;
    }

    /**
     * Returns the amount paid.
     * @return The amount.
     */
    public double getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return "Payment [transactionId=" + transactionId + ", amount=" + String.format("%.2f", amount) + ", status=" + status + ", paymentTime=" + paymentTime + "]";
    }
}

/**
 * The central class managing the entire parking system.
 */
class ParkingLot {
    private String name;
    private String address;
    private List<ParkingFloor> floors;
    private List<Gate> entryGates;
    private List<Gate> exitGates;
    private PricingStrategy pricingStrategy;
    private Map<String, Ticket> activeTickets; // Key: ticket ID, Value: Ticket object

    /**
     * Constructor for ParkingLot.
     * @param name Name of the parking lot.
     * @param address Address of the parking lot.
     * @param strategy The initial pricing strategy for the parking lot.
     */
    public ParkingLot(String name, String address, PricingStrategy strategy) {
        this.name = name;
        this.address = address;
        this.floors = new ArrayList<>();
        this.entryGates = new ArrayList<>();
        this.exitGates = new ArrayList<>();
        this.pricingStrategy = strategy;
        this.activeTickets = new HashMap<>();
        System.out.println("Parking Lot '" + name + "' created at " + address);
    }

    /**
     * Adds a parking floor to the lot.
     * @param floor The ParkingFloor to add.
     */
    public void addFloor(ParkingFloor floor) {
        floors.add(floor);
        System.out.println("Added Floor " + floor.getFloorNumber() + " to " + name);
    }

    /**
     * Adds an entry or exit gate to the lot.
     * @param gate The Gate to add.
     */
    public void addGate(Gate gate) {
        if (gate.getType() == GateType.ENTRY) {
            entryGates.add(gate);
        } else {
            exitGates.add(gate);
        }
        System.out.println("Added " + gate.getType() + " Gate " + gate.getGateId() + " to " + name);
    }

    /**
     * Handles vehicle entry. Finds a spot, assigns vehicle, generates ticket.
     * @param vehicle The vehicle entering the lot.
     * @return The generated Ticket, or null if no spot is available.
     */
    public Ticket parkVehicle(Vehicle vehicle) {
        ParkingSpot foundSpot = null;
        // Iterate through floors to find an available spot
        for (ParkingFloor floor : floors) {
            foundSpot = floor.findAvailableSpot(vehicle.getType());
            if (foundSpot != null) {
                // Set vehicle entry time before parking
                vehicle.setEntryTime(System.currentTimeMillis());
                if (floor.parkVehicle(vehicle, foundSpot)) {
                    String ticketId = UUID.randomUUID().toString();
                    Ticket ticket = new Ticket(ticketId, vehicle, vehicle.getEntryTime(), foundSpot);
                    activeTickets.put(ticketId, ticket);
                    System.out.println("Generated Ticket: " + ticketId + " for " + vehicle.getLicensePlate());
                    return ticket;
                }
            }
        }
        System.out.println("Parking lot is full for " + vehicle.getType() + " vehicles. " + vehicle.getLicensePlate() + " cannot be parked.");
        return null; // No spot available
    }

    /**
     * Handles vehicle exit and payment.
     * @param ticket The ticket presented by the exiting vehicle.
     * @param paymentStrategy The payment strategy to use for this transaction.
     * @return A Payment object indicating the transaction status, or null if ticket is invalid or payment fails.
     */
    public Payment unparkVehicle(Ticket ticket, PaymentStrategy paymentStrategy) {
        if (!activeTickets.containsKey(ticket.getTicketId())) {
            System.out.println("Error: Invalid or expired ticket " + ticket.getTicketId());
            return null;
        }

        Ticket activeTicket = activeTickets.get(ticket.getTicketId());
        long exitTime = System.currentTimeMillis();
        double amountDue = pricingStrategy.calculatePrice(activeTicket.getEntryTime(), exitTime, activeTicket.getVehicle().getType());

        System.out.println("Vehicle " + activeTicket.getVehicle().getLicensePlate() + " exiting. Amount due: $" + String.format("%.2f", amountDue));

        // Process payment using the provided strategy
        String transactionId = UUID.randomUUID().toString();
        Payment payment = new Payment(transactionId, activeTicket, amountDue, paymentStrategy);
        if (payment.processPayment()) {
            // Remove vehicle from spot
            activeTicket.getSpot().removeVehicle();
            // Remove ticket from active tickets
            activeTickets.remove(activeTicket.getTicketId());
            System.out.println("Vehicle " + activeTicket.getVehicle().getLicensePlate() + " successfully unparked.");
            return payment;
        } else {
            System.out.println("Payment failed for ticket " + activeTicket.getTicketId());
            return null;
        }
    }

    /**
     * Returns total available spots for a specific type across all floors.
     * @param type The ParkingSpotType to check.
     * @return The total count of available spots.
     */
    public int getAvailableSpots(ParkingSpotType type) {
        int totalAvailable = 0;
        for (ParkingFloor floor : floors) {
            totalAvailable += floor.getAvailableSpotsCount(type);
        }
        return totalAvailable;
    }

    /**
     * Allows changing the pricing strategy dynamically.
     * @param strategy The new PricingStrategy to apply.
     */
    public void setPricingStrategy(PricingStrategy strategy) {
        this.pricingStrategy = strategy;
        System.out.println("Pricing strategy updated to: " + strategy.getClass().getSimpleName());
    }

    /**
     * Returns the list of entry gates.
     * @return List of entry gates.
     */
    public List<Gate> getEntryGates() {
        return entryGates;
    }

    /**
     * Returns the list of exit gates.
     * @return List of exit gates.
     */
    public List<Gate> getExitGates() {
        return exitGates;
    }
}

/**
 * Represents an entry or exit point of the parking lot.
 */
class Gate {
    private String gateId;
    private GateType type;
    private ParkingLot parkingLot; // Reference to the parking lot it belongs to

    /**
     * Constructor for Gate.
     * @param gateId Unique ID for the gate.
     * @param type Type of gate (ENTRY or EXIT).
     * @param parkingLot Reference to the parking lot this gate serves.
     */
    public Gate(String gateId, GateType type, ParkingLot parkingLot) {
        this.gateId = gateId;
        this.type = type;
        this.parkingLot = parkingLot;
    }

    /**
     * For entry gates. Handles vehicle entry and issues a ticket.
     * @param vehicle The vehicle entering.
     * @return The generated Ticket, or null if parking is not possible.
     */
    public Ticket processEntry(Vehicle vehicle) {
        if (type == GateType.ENTRY) {
            System.out.println("\nGate " + gateId + " (ENTRY): Vehicle " + vehicle.getLicensePlate() + " attempting to enter.");
            return parkingLot.parkVehicle(vehicle);
        } else {
            System.out.println("Error: This is an EXIT gate. Cannot process entry.");
            return null;
        }
    }

    /**
     * For exit gates. Handles vehicle exit and payment processing.
     * @param ticket The ticket presented by the exiting vehicle.
     * @param paymentStrategy The payment strategy to use for this transaction.
     * @return A Payment object, or null if exit failed.
     */
    public Payment processExit(Ticket ticket, PaymentStrategy paymentStrategy) {
        if (type == GateType.EXIT) {
            System.out.println("\nGate " + gateId + " (EXIT): Vehicle with Ticket " + ticket.getTicketId() + " attempting to exit.");
            return parkingLot.unparkVehicle(ticket, paymentStrategy);
        } else {
            System.out.println("Error: This is an ENTRY gate. Cannot process exit.");
            return null;
        }
    }

    /**
     * Returns the gate ID.
     * @return The gate ID.
     */
    public String getGateId() {
        return gateId;
    }

    /**
     * Returns the type of the gate.
     * @return The GateType.
     */
    public GateType getType() {
        return type;
    }
}

/**
 * Provides real-time information on available parking spots.
 */
class DisplayBoard {
    private ParkingLot parkingLot;

    /**
     * Constructor for DisplayBoard.
     * @param parkingLot Reference to the parking lot whose information is to be displayed.
     */
    public DisplayBoard(ParkingLot parkingLot) {
        this.parkingLot = parkingLot;
    }

    /**
     * Fetches available spots from parkingLot and displays them.
     */
    public void updateDisplay() {
        System.out.println("\n--- Parking Availability ---");
        for (ParkingSpotType type : ParkingSpotType.values()) {
            System.out.println(type + " Spots Available: " + parkingLot.getAvailableSpots(type));
        }
        System.out.println("----------------------------");
    }
}

/**
 * Main class to demonstrate the Parking Lot System functionality.
 */
public class ParkingLotLLD {
    public static void main(String[] args) throws InterruptedException {
        // 1. Setup Pricing Strategy
        Map<VehicleType, Double> hourlyRates = new HashMap<>();
        hourlyRates.put(VehicleType.CAR, 2.0);
        hourlyRates.put(VehicleType.BIKE, 1.0);
        hourlyRates.put(VehicleType.TRUCK, 3.5);
        hourlyRates.put(VehicleType.VAN, 2.5);
        hourlyRates.put(VehicleType.ELECTRIC_CAR, 2.2);
        PricingStrategy hourlyPricing = new HourlyPricing(hourlyRates);

        // 2. Create Parking Lot
        ParkingLot myParkingLot = new ParkingLot("Central Park", "123 Main St", hourlyPricing);

        // 3. Add Floors and Spots
        ParkingFloor floor1 = new ParkingFloor(1);
        floor1.addSpot(new ParkingSpot("F1-S001", ParkingSpotType.SMALL));
        floor1.addSpot(new ParkingSpot("F1-S002", ParkingSpotType.MEDIUM));
        floor1.addSpot(new ParkingSpot("F1-S003", ParkingSpotType.LARGE));
        floor1.addSpot(new ParkingSpot("F1-S004", ParkingSpotType.ELECTRIC_CHARGING));
        floor1.addSpot(new ParkingSpot("F1-S005", ParkingSpotType.MEDIUM));
        myParkingLot.addFloor(floor1);

        ParkingFloor floor2 = new ParkingFloor(2);
        floor2.addSpot(new ParkingSpot("F2-S001", ParkingSpotType.MEDIUM));
        floor2.addSpot(new ParkingSpot("F2-S002", ParkingSpotType.MEDIUM));
        floor2.addSpot(new ParkingSpot("F2-S003", ParkingSpotType.LARGE));
        myParkingLot.addFloor(floor2);

        // 4. Add Gates
        Gate entryGate1 = new Gate("E1", GateType.ENTRY, myParkingLot);
        Gate exitGate1 = new Gate("X1", GateType.EXIT, myParkingLot);
        myParkingLot.addGate(entryGate1);
        myParkingLot.addGate(exitGate1);

        // 5. Create Display Board
        DisplayBoard displayBoard = new DisplayBoard(myParkingLot);
        displayBoard.updateDisplay();

        // --- Simulation of Parking Lot Operations ---

        // Vehicle 1: Car enters
        Vehicle car1 = new Vehicle("KA01AB1234", VehicleType.CAR);
        Ticket ticket1 = entryGate1.processEntry(car1);
        displayBoard.updateDisplay();

        // Vehicle 2: Bike enters
        Vehicle bike1 = new Vehicle("KA02CD5678", VehicleType.BIKE);
        Ticket ticket2 = entryGate1.processEntry(bike1);
        displayBoard.updateDisplay();

        // Vehicle 3: Truck enters
        Vehicle truck1 = new Vehicle("KA03EF9012", VehicleType.TRUCK);
        Ticket ticket3 = entryGate1.processEntry(truck1);
        displayBoard.updateDisplay();

        // Vehicle 4: Electric Car enters
        Vehicle electricCar1 = new Vehicle("KA04GH3456", VehicleType.ELECTRIC_CAR);
        Ticket ticket4 = entryGate1.processEntry(electricCar1);
        displayBoard.updateDisplay();

        // Simulate some time passing
        System.out.println("\n--- Simulating 3 hours passing ---");
        Thread.sleep(TimeUnit.SECONDS.toMillis(3)); // Simulate 3 seconds for demonstration, actual 3 hours

        // Vehicle 1 exits with Cash Payment
        if (ticket1 != null) {
            PaymentStrategy cashPayment = new CashPayment();
            Payment payment1 = exitGate1.processExit(ticket1, cashPayment);
            if (payment1 != null && payment1.getStatus() == PaymentStatus.COMPLETED) {
                System.out.println("Car 1 exited successfully. Paid: $" + String.format("%.2f", payment1.getAmount()));
            }
        }
        displayBoard.updateDisplay();

        // Simulate more time passing
        System.out.println("\n--- Simulating 1 hour passing ---");
        Thread.sleep(TimeUnit.SECONDS.toMillis(1)); // Simulate 1 second for demonstration, actual 1 hour

        // Vehicle 2 exits with Card Payment
        if (ticket2 != null) {
            PaymentStrategy cardPayment = new CardPayment();
            Payment payment2 = exitGate1.processExit(ticket2, cardPayment);
            if (payment2 != null && payment2.getStatus() == PaymentStatus.COMPLETED) {
                System.out.println("Bike 1 exited successfully. Paid: $" + String.format("%.2f", payment2.getAmount()));
            }
        }
        displayBoard.updateDisplay();

        // Try to park a vehicle when spots are limited
        Vehicle van1 = new Vehicle("KA05IJ6789", VehicleType.VAN);
        Ticket ticket5 = entryGate1.processEntry(van1);
        displayBoard.updateDisplay();

        // Change pricing strategy to Daily
        System.out.println("\n--- Changing pricing strategy to Daily ---");
        Map<VehicleType, Double> dailyRates = new HashMap<>();
        dailyRates.put(VehicleType.CAR, 15.0);
        dailyRates.put(VehicleType.BIKE, 8.0);
        dailyRates.put(VehicleType.TRUCK, 25.0);
        dailyRates.put(VehicleType.VAN, 18.0);
        dailyRates.put(VehicleType.ELECTRIC_CAR, 16.0);
        PricingStrategy dailyPricing = new DailyPricing(dailyRates);
        myParkingLot.setPricingStrategy(dailyPricing);

        // Simulate more time passing
        System.out.println("\n--- Simulating 2 days passing ---");
        Thread.sleep(TimeUnit.SECONDS.toMillis(2)); // Simulate 2 seconds for demonstration, actual 2 days

        // Vehicle 3 exits (now with daily pricing)
        if (ticket3 != null) {
            PaymentStrategy cashPayment = new CashPayment(); // Can use any payment strategy
            Payment payment3 = exitGate1.processExit(ticket3, cashPayment);
            if (payment3 != null && payment3.getStatus() == PaymentStatus.COMPLETED) {
                System.out.println("Truck 1 exited successfully. Paid: $" + String.format("%.2f", payment3.getAmount()));
            }
        }
        displayBoard.updateDisplay();
    }
}
