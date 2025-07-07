// Package for core model entities
package com.vehiclerental.core;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit; // For date calculations
import java.util.Objects;
import java.util.UUID; // For generating unique IDs
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.Collection;
import java.util.Set; // For vehicle booking schedule

/**
 * Represents a vehicle in the rental system.
 * Simplified for interview focus.
 */
class Vehicle {
    private String registrationNumber;
    private String model;
    private String make;
    private int year;
    private double rentalPricePerDay;
    private String type; // e.g., "Car", "Bike", "Scooter"
    private String category; // e.g., "Economy", "SUV", "Luxury"
    private String color; // Added for preferences

    public Vehicle(String registrationNumber, String model, String make, int year,
                   double rentalPricePerDay, String type, String category, String color) {
        this.registrationNumber = registrationNumber;
        this.model = model;
        this.make = make;
        this.year = year;
        this.rentalPricePerDay = rentalPricePerDay;
        this.type = type;
        this.category = category;
        this.color = color;
    }

    // Getters (essential for accessing data)
    public String getRegistrationNumber() { return registrationNumber; }
    public String getModel() { return model; }
    public String getMake() { return make; }
    public int getYear() { return year; }
    public double getRentalPricePerDay() { return rentalPricePerDay; }
    public String getType() { return type; }
    public String getCategory() { return category; }
    public String getColor() { return color; }


    // Setters (minimal, only if needed for core logic)
    public void setRentalPricePerDay(double rentalPricePerDay) { this.rentalPricePerDay = rentalPricePerDay; }

    @Override
    public String toString() {
        return String.format("%s %s (%d) - Reg: %s, Type: %s, Category: %s, Price: %.2f/day",
                make, model, year, registrationNumber, type, category, rentalPricePerDay);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vehicle vehicle = (Vehicle) o;
        return Objects.equals(registrationNumber, vehicle.registrationNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(registrationNumber);
    }
}

/**
 * Represents a rental location (store) in the system.
 */
class RentalLocation {
    private String locationId;
    private String name;
    private String address;
    // Vehicles available at this specific location
    private Map<String, Vehicle> vehicles;

    public RentalLocation(String locationId, String name, String address) {
        this.locationId = locationId;
        this.name = name;
        this.address = address;
        this.vehicles = new HashMap<>();
    }

    // Getters
    public String getLocationId() { return locationId; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public Map<String, Vehicle> getVehicles() { return vehicles; } // Expose for main service logic

    /**
     * Adds a vehicle to this location's inventory.
     */
    public void addVehicle(Vehicle vehicle) {
        if (vehicle != null) {
            vehicles.put(vehicle.getRegistrationNumber(), vehicle);
            System.out.println("Added vehicle " + vehicle.getRegistrationNumber() + " to " + name);
        }
    }

    /**
     * Removes a vehicle from this location's inventory.
     */
    public Vehicle removeVehicle(String registrationNumber) {
        Vehicle removedVehicle = vehicles.remove(registrationNumber);
        if (removedVehicle != null) {
            System.out.println("Removed vehicle " + registrationNumber + " from " + name);
        }
        return removedVehicle;
    }

    /**
     * Retrieves a vehicle by its registration number from this location.
     */
    public Vehicle getVehicle(String registrationNumber) {
        return vehicles.get(registrationNumber);
    }

    /**
     * Get all vehicles at this location.
     */
    public Collection<Vehicle> getAllVehicles() {
        return vehicles.values();
    }

    @Override
    public String toString() {
        return String.format("Location ID: %s, Name: %s, Address: %s, Vehicles: %d",
                locationId, name, address, vehicles.size());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RentalLocation that = (RentalLocation) o;
        return Objects.equals(locationId, that.locationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(locationId);
    }
}

/**
 * Represents a user of the rental system.
 */
class User {
    private String userId;
    private String name;
    private String email;

    public User(String userId, String name, String email) {
        this.userId = userId;
        this.name = name;
        this.email = email;
    }

    // Getters
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }

    @Override
    public String toString() {
        return String.format("User ID: %s, Name: %s, Email: %s", userId, name, email);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(userId, user.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
}

/**
 * Represents a vehicle rental reservation.
 */
class Reservation {
    private String reservationId;
    private String userId;
    private String vehicleRegistrationNumber;
    private String pickupLocationId;
    private String returnLocationId;
    private LocalDate pickupDate;
    private LocalDate returnDate;
    private double totalPrice;
    private String status; // "PENDING", "CONFIRMED", "CANCELLED", "COMPLETED"

    public Reservation(String reservationId, String userId, String vehicleRegistrationNumber,
                       String pickupLocationId, String returnLocationId,
                       LocalDate pickupDate, LocalDate returnDate, double totalPrice) {
        this.reservationId = reservationId;
        this.userId = userId;
        this.vehicleRegistrationNumber = vehicleRegistrationNumber;
        this.pickupLocationId = pickupLocationId;
        this.returnLocationId = returnLocationId;
        this.pickupDate = pickupDate;
        this.returnDate = returnDate;
        this.totalPrice = totalPrice;
        this.status = "PENDING"; // Default status
    }

    // Getters
    public String getReservationId() { return reservationId; }
    public String getUserId() { return userId; }
    public String getVehicleRegistrationNumber() { return vehicleRegistrationNumber; }
    public String getPickupLocationId() { return pickupLocationId; }
    public String getReturnLocationId() { return returnLocationId; }
    public LocalDate getPickupDate() { return pickupDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public double getTotalPrice() { return totalPrice; }
    public String getStatus() { return status; }

    // Setters for modifiable fields (needed for modifyReservation)
    public void setPickupDate(LocalDate pickupDate) { this.pickupDate = pickupDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
    public void setStatus(String status) { this.status = status; }
    public void setVehicleRegistrationNumber(String vehicleRegistrationNumber) { this.vehicleRegistrationNumber = vehicleRegistrationNumber; }
    public void setPickupLocationId(String pickupLocationId) { this.pickupLocationId = pickupLocationId; }
    public void setReturnLocationId(String returnLocationId) { this.returnLocationId = returnLocationId; }


    @Override
    public String toString() {
        return String.format("Res ID: %s, User: %s, Vehicle: %s, Dates: %s to %s, Total: %.2f, Status: %s",
                reservationId, userId, vehicleRegistrationNumber, pickupDate, returnDate, totalPrice, status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reservation that = (Reservation) o;
        return Objects.equals(reservationId, that.reservationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reservationId);
    }
}

/**
 * Represents a payment transaction.
 * Simplified for interview.
 */
class Payment {
    private String paymentId;
    private String reservationId;
    private double amount;
    private LocalDate paymentDate;
    private String method; // e.g., "CREDIT_CARD", "DEBIT_CARD"
    private String status; // "PENDING", "COMPLETED", "FAILED"

    public Payment(String paymentId, String reservationId, double amount, String method) {
        this.paymentId = paymentId;
        this.reservationId = reservationId;
        this.amount = amount;
        this.paymentDate = LocalDate.now();
        this.method = method;
        this.status = "PENDING";
    }

    // Getters
    public String getPaymentId() { return paymentId; }
    public String getReservationId() { return reservationId; }
    public double getAmount() { return amount; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public String getMethod() { return method; }
    public String getStatus() { return status; }

    // Setter for status
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return String.format("Payment ID: %s, Res: %s, Amt: %.2f, Method: %s, Status: %s",
                paymentId, reservationId, amount, method, status);
    }
}

/**
 * Represents an invoice for a rental.
 * Simplified for interview.
 */
class Invoice {
    private String invoiceId;
    private String reservationId;
    private double amount;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private boolean paid;

    public Invoice(String invoiceId, String reservationId, double amount, LocalDate dueDate) {
        this.invoiceId = invoiceId;
        this.reservationId = reservationId;
        this.amount = amount;
        this.issueDate = LocalDate.now();
        this.dueDate = dueDate;
        this.paid = false;
    }

    // Getters
    public String getInvoiceId() { return invoiceId; }
    public String getReservationId() { return reservationId; }
    public double getAmount() { return amount; }
    public LocalDate getIssueDate() { return issueDate; }
    public LocalDate getDueDate() { return dueDate; }
    public boolean isPaid() { return paid; }

    // Setter for paid status
    public void setPaid(boolean paid) { this.paid = paid; }

    @Override
    public String toString() {
        return String.format("Invoice ID: %s, Res: %s, Amt: %.2f, Due: %s, Paid: %b",
                invoiceId, reservationId, amount, dueDate, paid);
    }
}


/**
 * Main service class for the vehicle rental system.
 * Combines inventory and reservation management for simplicity in an interview.
 */
class RentalService {
    // Stores all rental locations by their ID
    private Map<String, RentalLocation> locations;
    // Stores all reservations by their ID
    private Map<String, Reservation> reservations;
    // Stores all payments by their ID
    private Map<String, Payment> payments;
    // Stores all invoices by their ID
    private Map<String, Invoice> invoices;


    // A simple in-memory structure to track vehicle-specific bookings for conflict checking.
    // Key: Vehicle Registration Number, Value: List of Reservations for that vehicle
    // This simplifies the date conflict check to iterating through this list.
    private Map<String, List<Reservation>> vehicleBookingSchedule;

    public RentalService() {
        this.locations = new HashMap<>();
        this.reservations = new HashMap<>();
        this.payments = new HashMap<>();
        this.invoices = new HashMap<>();
        this.vehicleBookingSchedule = new HashMap<>();
    }

    // --- Location and Vehicle Management ---

    /**
     * Adds a new rental location to the system.
     */
    public void addLocation(RentalLocation location) {
        if (location != null && !locations.containsKey(location.getLocationId())) {
            locations.put(location.getLocationId(), location);
            System.out.println("Location added: " + location.getName());
        } else {
            System.out.println("Failed to add location: " + (location == null ? "null" : "ID already exists or null location"));
        }
    }

    /**
     * Adds a vehicle to a specific rental location.
     */
    public void addVehicleToLocation(String locationId, Vehicle vehicle) {
        RentalLocation location = locations.get(locationId);
        if (location != null) {
            location.addVehicle(vehicle);
        } else {
            System.out.println("Location not found: " + locationId);
        }
    }

    /**
     * Retrieves a vehicle from a specific location.
     */
    public Vehicle getVehicleAtLocation(String locationId, String registrationNumber) {
        RentalLocation location = locations.get(locationId);
        if (location != null) {
            return location.getVehicle(registrationNumber);
        }
        return null;
    }

    /**
     * Searches for vehicles available at a location within a date range and by type/category.
     * This is the core search functionality.
     * @param sortByPriceAscending true to sort by price ascending, false for descending.
     */
    public List<Vehicle> findAvailableVehicles(String locationId, LocalDate startDate, LocalDate endDate,
                                                String type, String category, String color, boolean sortByPriceAscending) {
        List<Vehicle> availableVehicles = new ArrayList<>();
        RentalLocation location = locations.get(locationId);

        if (location == null) {
            System.out.println("Location not found: " + locationId);
            return availableVehicles;
        }

        for (Vehicle vehicle : location.getAllVehicles()) {
            // Apply filters
            boolean matches = true;
            if (type != null && !vehicle.getType().equalsIgnoreCase(type)) matches = false;
            if (category != null && !vehicle.getCategory().equalsIgnoreCase(category)) matches = false;
            if (color != null && !vehicle.getColor().equalsIgnoreCase(color)) matches = false;


            // Check availability for the date range
            if (matches && isVehicleAvailableForDates(vehicle.getRegistrationNumber(), startDate, endDate)) {
                availableVehicles.add(vehicle);
            }
        }

        // Apply sorting
        availableVehicles.sort((v1, v2) -> {
            if (sortByPriceAscending) {
                return Double.compare(v1.getRentalPricePerDay(), v2.getRentalPricePerDay());
            } else {
                return Double.compare(v2.getRentalPricePerDay(), v1.getRentalPricePerDay());
            }
        });

        return availableVehicles;
    }

    /**
     * Helper method to check if a vehicle is available for the given date range.
     * Iterates through existing reservations for the vehicle to find overlaps.
     */
    private boolean isVehicleAvailableForDates(String vehicleRegNo, LocalDate desiredPickup, LocalDate desiredReturn) {
        List<Reservation> vehicleBookings = vehicleBookingSchedule.getOrDefault(vehicleRegNo, new ArrayList<>());

        for (Reservation existingReservation : vehicleBookings) {
            // Only consider CONFIRMED or PENDING reservations for conflicts
            if (existingReservation.getStatus().equals("CONFIRMED") || existingReservation.getStatus().equals("PENDING")) {
                // Check for overlap: (StartA <= EndB) and (EndA >= StartB)
                boolean overlap = (desiredPickup.isBefore(existingReservation.getReturnDate()) || desiredPickup.isEqual(existingReservation.getReturnDate())) &&
                                  (desiredReturn.isAfter(existingReservation.getPickupDate()) || desiredReturn.isEqual(existingReservation.getPickupDate()));
                if (overlap) {
                    return false; // Conflict found
                }
            }
        }
        return true; // No conflicts found
    }

    // --- Reservation Management ---

    /**
     * Creates a new reservation for a vehicle.
     */
    public Reservation createReservation(String userId, String vehicleRegNo,
                                         String pickupLocationId, String returnLocationId,
                                         LocalDate pickupDate, LocalDate returnDate) {

        // Basic validation
        if (pickupDate.isAfter(returnDate) || pickupDate.isBefore(LocalDate.now())) {
            System.out.println("Error: Invalid dates for reservation.");
            return null;
        }

        Vehicle vehicle = getVehicleAtLocation(pickupLocationId, vehicleRegNo);
        if (vehicle == null) {
            System.out.println("Error: Vehicle " + vehicleRegNo + " not found at location " + pickupLocationId);
            return null;
        }

        if (!isVehicleAvailableForDates(vehicleRegNo, pickupDate, returnDate)) {
            System.out.println("Error: Vehicle " + vehicleRegNo + " is already booked for the requested dates.");
            return null;
        }

        // Calculate total price (simple calculation)
        long numberOfDays = ChronoUnit.DAYS.between(pickupDate, returnDate) + 1;
        double totalPrice = vehicle.getRentalPricePerDay() * numberOfDays;

        String reservationId = UUID.randomUUID().toString();
        Reservation newReservation = new Reservation(reservationId, userId, vehicleRegNo,
                                                     pickupLocationId, returnLocationId,
                                                     pickupDate, returnDate, totalPrice);

        reservations.put(reservationId, newReservation);
        vehicleBookingSchedule.computeIfAbsent(vehicleRegNo, k -> new ArrayList<>()).add(newReservation);

        newReservation.setStatus("CONFIRMED"); // Auto-confirm for simplicity

        System.out.println("Reservation created: " + newReservation);
        return newReservation;
    }

    /**
     * Modifies an existing reservation.
     * This method allows changing dates, locations, and even the vehicle for a reservation.
     * It includes re-validation for availability.
     *
     * @param reservationId The ID of the reservation to modify.
     * @param newPickupDate Optional: New pickup date. If null, original date is kept.
     * @param newReturnDate Optional: New return date. If null, original date is kept.
     * @param newVehicleRegNo Optional: New vehicle registration number. If null, original vehicle is kept.
     * @param newPickupLocationId Optional: New pickup location ID. If null, original location is kept.
     * @param newReturnLocationId Optional: New return location ID. If null, original location is kept.
     * @return The updated Reservation object, or null if modification fails.
     */
    public Reservation modifyReservation(String reservationId, LocalDate newPickupDate, LocalDate newReturnDate,
                                         String newVehicleRegNo, String newPickupLocationId, String newReturnLocationId) {
        Reservation reservation = reservations.get(reservationId);
        if (reservation == null) {
            System.out.println("Error: Reservation " + reservationId + " not found for modification.");
            return null;
        }

        // Do not allow modification of cancelled or completed reservations
        if (reservation.getStatus().equals("CANCELLED") || reservation.getStatus().equals("COMPLETED")) {
            System.out.println("Error: Cannot modify a " + reservation.getStatus() + " reservation.");
            return null;
        }

        // Store original values for potential rollback or comparison
        LocalDate originalPickupDate = reservation.getPickupDate();
        LocalDate originalReturnDate = reservation.getReturnDate();
        String originalVehicleRegNo = reservation.getVehicleRegistrationNumber();
        String originalPickupLocationId = reservation.getPickupLocationId();
        String originalReturnLocationId = reservation.getReturnLocationId();

        // Determine effective new values (use new value if provided, else keep original)
        LocalDate effectivePickupDate = (newPickupDate != null) ? newPickupDate : originalPickupDate;
        LocalDate effectiveReturnDate = (newReturnDate != null) ? newReturnDate : originalReturnDate;
        String effectiveVehicleRegNo = (newVehicleRegNo != null) ? newVehicleRegNo : originalVehicleRegNo;
        String effectivePickupLocationId = (newPickupLocationId != null) ? newPickupLocationId : originalPickupLocationId;
        String effectiveReturnLocationId = (newReturnLocationId != null) ? newReturnLocationId : originalReturnLocationId;

        // --- Step 1: Validate new dates and locations ---
        if (effectivePickupDate.isAfter(effectiveReturnDate) || effectivePickupDate.isBefore(LocalDate.now())) {
            System.out.println("Error: Invalid new dates for reservation modification.");
            return null;
        }

        // Check if new locations are valid
        if (locations.get(effectivePickupLocationId) == null || locations.get(effectiveReturnLocationId) == null) {
            System.out.println("Error: New pickup or return location not found.");
            return null;
        }

        // --- Step 2: Temporarily remove the current reservation from the schedule ---
        // This is crucial to prevent the reservation from conflicting with itself during re-validation.
        List<Reservation> oldVehicleBookings = vehicleBookingSchedule.get(originalVehicleRegNo);
        if (oldVehicleBookings != null) {
            oldVehicleBookings.remove(reservation);
            if (oldVehicleBookings.isEmpty()) {
                vehicleBookingSchedule.remove(originalVehicleRegNo);
            }
        }

        // --- Step 3: Validate new vehicle availability ---
        Vehicle newVehicle = getVehicleAtLocation(effectivePickupLocationId, effectiveVehicleRegNo);
        if (newVehicle == null) {
            System.out.println("Error: New vehicle " + effectiveVehicleRegNo + " not found at location " + effectivePickupLocationId);
            // Rollback schedule change before returning null
            vehicleBookingSchedule.computeIfAbsent(originalVehicleRegNo, k -> new ArrayList<>()).add(reservation);
            return null;
        }

        // Check for conflicts with other bookings for the *new* vehicle and *new* dates
        if (!isVehicleAvailableForDates(effectiveVehicleRegNo, effectivePickupDate, effectiveReturnDate)) {
            System.out.println("Error: New dates or vehicle conflict with existing bookings.");
            // Rollback schedule change before returning null
            vehicleBookingSchedule.computeIfAbsent(originalVehicleRegNo, k -> new ArrayList<>()).add(reservation);
            return null;
        }

        // --- Step 4: Apply changes to the reservation object ---
        reservation.setPickupDate(effectivePickupDate);
        reservation.setReturnDate(effectiveReturnDate);
        reservation.setPickupLocationId(effectivePickupLocationId);
        reservation.setReturnLocationId(effectiveReturnLocationId);
        reservation.setVehicleRegistrationNumber(effectiveVehicleRegNo); // Update vehicle if changed

        // --- Step 5: Recalculate total price based on new dates and potentially new vehicle price ---
        long numberOfDays = ChronoUnit.DAYS.between(effectivePickupDate, effectiveReturnDate) + 1;
        double newTotalPrice = newVehicle.getRentalPricePerDay() * numberOfDays;
        reservation.setTotalPrice(newTotalPrice);

        // --- Step 6: Add the modified reservation back to the schedule for the (potentially new) vehicle ---
        vehicleBookingSchedule.computeIfAbsent(effectiveVehicleRegNo, k -> new ArrayList<>()).add(reservation);


        System.out.println("Reservation " + reservationId + " modified successfully: " + reservation);
        return reservation;
    }


    /**
     * Cancels an existing reservation.
     */
    public boolean cancelReservation(String reservationId) {
        Reservation reservation = reservations.get(reservationId);
        if (reservation == null) {
            System.out.println("Error: Reservation " + reservationId + " not found.");
            return false;
        }

        if (reservation.getStatus().equals("CANCELLED") || reservation.getStatus().equals("COMPLETED")) {
            System.out.println("Error: Reservation " + reservationId + " cannot be cancelled (already " + reservation.getStatus() + ").");
            return false;
        }

        reservation.setStatus("CANCELLED");
        // Remove from the booking schedule (important for availability)
        List<Reservation> bookings = vehicleBookingSchedule.get(reservation.getVehicleRegistrationNumber());
        if (bookings != null) {
            bookings.remove(reservation); // Remove by object equality
            if (bookings.isEmpty()) {
                vehicleBookingSchedule.remove(reservation.getVehicleRegistrationNumber());
            }
        }
        System.out.println("Reservation " + reservationId + " cancelled.");
        return true;
    }

    /**
     * Retrieves a reservation by its ID.
     */
    public Reservation getReservation(String reservationId) {
        return reservations.get(reservationId);
    }

    /**
     * Gets all reservations for a specific user.
     */
    public List<Reservation> getReservationsForUser(String userId) {
        return reservations.values().stream()
                .filter(r -> r.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    /**
     * Simulates completing a reservation (e.g., after vehicle return).
     */
    public boolean completeReservation(String reservationId) {
        Reservation reservation = reservations.get(reservationId);
        if (reservation == null) {
            System.out.println("Error: Reservation " + reservationId + " not found.");
            return false;
        }
        if (reservation.getStatus().equals("COMPLETED")) {
            System.out.println("Reservation " + reservationId + " already completed.");
            return false;
        }
        reservation.setStatus("COMPLETED");
        // For simplicity, we don't remove from schedule immediately, as `isVehicleAvailableForDates`
        // already filters out COMPLETED/CANCELLED. In a real system, you might clean this up.
        System.out.println("Reservation " + reservationId + " completed.");
        return true;
    }

    // --- Billing/Invoicing & Payment Processing ---

    /**
     * Processes a payment for a given reservation.
     * In an interview, this would be a high-level simulation.
     * Real-world would involve payment gateways, security, etc.
     *
     * @param reservationId The ID of the reservation.
     * @param amount The amount to be paid.
     * @param method The payment method (e.g., "CREDIT_CARD", "DEBIT_CARD").
     * @return The created Payment object, or null if processing fails.
     */
    public Payment processPayment(String reservationId, double amount, String method) {
        Reservation reservation = reservations.get(reservationId);

        if (reservation == null) {
            System.out.println("Payment failed: Reservation " + reservationId + " not found.");
            return null;
        }
        if (reservation.getStatus().equals("CANCELLED")) {
            System.out.println("Payment failed: Reservation " + reservationId + " is cancelled.");
            return null;
        }
        // Simple check: amount must be at least the reservation total
        if (amount < reservation.getTotalPrice()) {
             System.out.println("Payment failed: Amount (%.2f) is less than total price (%.2f) for reservation %s".formatted(amount, reservation.getTotalPrice(), reservationId));
             return null;
        }

        String paymentId = UUID.randomUUID().toString();
        Payment payment = new Payment(paymentId, reservationId, amount, method);

        // Simulate payment gateway interaction (always successful for low-level)
        payment.setStatus("COMPLETED");
        payments.put(paymentId, payment);

        // Update reservation status if payment completes it (e.g., from PENDING to CONFIRMED)
        if (reservation.getStatus().equals("PENDING")) {
            reservation.setStatus("CONFIRMED");
        }

        // Generate invoice and mark as paid
        Invoice invoice = generateInvoice(reservationId, amount);
        invoice.setPaid(true); // Mark invoice as paid immediately upon successful payment
        invoices.put(invoice.getInvoiceId(), invoice);

        System.out.println("Payment processed: " + payment);
        System.out.println("Invoice generated: " + invoice);
        return payment;
    }

    /**
     * Generates an invoice for a reservation.
     * This would typically be called after a successful payment.
     * @param reservationId The ID of the reservation.
     * @param amount The amount for the invoice.
     * @return The generated Invoice object.
     */
    private Invoice generateInvoice(String reservationId, double amount) {
        String invoiceId = UUID.randomUUID().toString();
        // Due date is 7 days from now for simplicity, or could be immediate if paid on creation
        LocalDate dueDate = LocalDate.now().plusDays(7);
        return new Invoice(invoiceId, reservationId, amount, dueDate);
    }

    /**
     * Retrieves a payment by its ID.
     */
    public Payment getPayment(String paymentId) {
        return payments.get(paymentId);
    }

    /**
     * Retrieves an invoice by its ID.
     */
    public Invoice getInvoice(String invoiceId) {
        return invoices.get(invoiceId);
    }

    // --- Utility/Helper Methods (can be skipped or simplified in interview if time is tight) ---

    /**
     * Gets all rental locations.
     */
    public Collection<RentalLocation> getAllLocations() {
        return locations.values();
    }

    /**
     * Gets all payments.
     */
    public Collection<Payment> getAllPayments() {
        return payments.values();
    }

    /**
     * Gets all invoices.
     */
    public Collection<Invoice> getAllInvoices() {
        return invoices.values();
    }
}

// Main class for demonstration
package com.vehiclerental;

import com.vehiclerental.core.*; // Import all core classes

import java.time.LocalDate;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("--- Vehicle Rental System Demo (Interview Version with Modifications & Payments) ---");

        // 1. Initialize the main service
        RentalService rentalService = new RentalService();

        // 2. Setup Locations
        RentalLocation downtownLocation = new RentalLocation("L001", "Downtown Rentals", "123 Main St, City A");
        RentalLocation airportLocation = new RentalLocation("L002", "Airport Express", "456 Airport Rd, City B");
        rentalService.addLocation(downtownLocation);
        rentalService.addLocation(airportLocation);

        // 3. Add Vehicles to Locations
        // Added 'color' attribute to Vehicle constructor
        Vehicle civic = new Vehicle("CAR001", "Civic", "Honda", 2022, 50.0, "Car", "Economy", "Silver");
        Vehicle crv = new Vehicle("CAR002", "CR-V", "Honda", 2023, 75.0, "Car", "SUV", "Blue");
        Vehicle ninja = new Vehicle("BIK001", "Ninja 400", "Kawasaki", 2021, 40.0, "Bike", "Sport", "Green");
        Vehicle vespa = new Vehicle("SCO001", "Vespa Sprint", "Piaggio", 2020, 25.0, "Scooter", "Standard", "Red");
        Vehicle luxurySedan = new Vehicle("CAR003", "E-Class", "Mercedes-Benz", 2024, 150.0, "Car", "Luxury", "Black"); // New vehicle

        rentalService.addVehicleToLocation("L001", civic);
        rentalService.addVehicleToLocation("L001", ninja);
        rentalService.addVehicleToLocation("L002", crv);
        rentalService.addVehicleToLocation("L002", vespa);
        rentalService.addVehicleToLocation("L001", luxurySedan); // Add new luxury car

        System.out.println("\n--- Current Vehicle Inventory ---");
        rentalService.getAllLocations().forEach(loc -> {
            System.out.println(loc.getName() + ":");
            loc.getAllVehicles().forEach(System.out::println);
        });

        // 4. Create Users (can be simple inline creation for demo)
        User user1 = new User("U001", "Alice Smith", "alice@example.com");
        User user2 = new User("U002", "Bob Johnson", "bob@example.com");

        // 5. Search for Vehicles
        System.out.println("\n--- Searching for available Economy Cars in Downtown (next 3 days) ---");
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDate threeDaysLater = today.plusDays(2); // Inclusive of start and end day
        LocalDate nextWeek = today.plusWeeks(1);
        LocalDate twoWeeksLater = today.plusWeeks(2);
        LocalDate threeWeeksLater = today.plusWeeks(3);


        List<Vehicle> availableEconomyCars = rentalService.findAvailableVehicles(
            "L001", today, threeDaysLater, "Car", "Economy", null, true); // Added color filter (null) and sorting
        if (availableEconomyCars.isEmpty()) {
            System.out.println("No economy cars available.");
        } else {
            availableEconomyCars.forEach(System.out::println);
        }

        System.out.println("\n--- Searching for all vehicles sorted by price descending ---");
        List<Vehicle> allVehiclesSorted = rentalService.findAvailableVehicles(
            "L001", today, today.plusMonths(1), null, null, null, false); // Search all types for a month, sort desc
        allVehiclesSorted.forEach(System.out::println);


        // 6. Create Reservations
        System.out.println("\n--- Creating Reservations ---");

        // Successful reservation for CAR001
        Reservation res1 = rentalService.createReservation(
            user1.getUserId(), "CAR001", "L001", "L001", today, tomorrow);

        // Attempt to double-book CAR001 for the same dates
        System.out.println("\n--- Attempting to double-book CAR001 (should fail) ---");
        Reservation res2 = rentalService.createReservation(
            user2.getUserId(), "CAR001", "L001", "L001", today, tomorrow);

        // Successful reservation for CRV002
        Reservation res3 = rentalService.createReservation(
            user2.getUserId(), "CAR002", "L002", "L002", nextWeek, twoWeeksLater);

        System.out.println("\n--- All Reservations for Alice ---");
        rentalService.getReservationsForUser(user1.getUserId()).forEach(System.out::println);

        // 7. Modify Reservation
        System.out.println("\n--- Modifying Reservation ---");
        if (res1 != null) {
            System.out.println("Original Res1: " + res1);
            // Modify dates of res1
            Reservation modifiedRes1 = rentalService.modifyReservation(res1.getReservationId(),
                today.plusDays(2), today.plusDays(4), null, null, null);
            System.out.println("Modified Res1 (dates): " + modifiedRes1);

            // Attempt to modify res1 to conflict with res3 (if res3 is still active)
            System.out.println("\n--- Attempting to modify Res1 to conflict with Res3 (should fail) ---");
            if (res3 != null && res3.getStatus().equals("CONFIRMED")) {
                rentalService.modifyReservation(res1.getReservationId(),
                    res3.getPickupDate(), res3.getReturnDate(), null, null, null);
            }

            // Modify res1 to a different vehicle (CAR003) at the same location
            System.out.println("\n--- Modifying Res1 to use CAR003 ---");
            Reservation modifiedRes1ToCar003 = rentalService.modifyReservation(res1.getReservationId(),
                null, null, "CAR003", "L001", "L001");
            System.out.println("Modified Res1 (to CAR003): " + modifiedRes1ToCar003);
        }


        // 8. Process Payments and Invoices
        System.out.println("\n--- Processing Payments and Generating Invoices ---");
        if (res1 != null && res1.getStatus().equals("CONFIRMED")) {
            System.out.println("Processing payment for Res1 (now using CAR003)");
            rentalService.processPayment(res1.getReservationId(), res1.getTotalPrice(), "CREDIT_CARD");
        } else {
            System.out.println("Res1 not in a state to process payment.");
        }

        if (res3 != null && res3.getStatus().equals("CONFIRMED")) {
            System.out.println("Processing payment for Res3");
            rentalService.processPayment(res3.getReservationId(), res3.getTotalPrice(), "DEBIT_CARD");
        } else {
            System.out.println("Res3 not in a state to process payment.");
        }

        System.out.println("\n--- All Payments ---");
        rentalService.getAllPayments().forEach(System.out::println);
        System.out.println("\n--- All Invoices ---");
        rentalService.getAllInvoices().forEach(System.out::println);


        // 9. Cancel Reservation
        System.out.println("\n--- Cancelling Reservation " + (res1 != null ? res1.getReservationId() : "N/A") + " ---");
        if (res1 != null) {
            rentalService.cancelReservation(res1.getReservationId());
        }

        System.out.println("\n--- All Reservations for Alice after cancellation ---");
        rentalService.getReservationsForUser(user1.getUserId()).forEach(System.out::println);

        // 10. Complete Reservation (if not cancelled)
        System.out.println("\n--- Completing Reservation " + (res3 != null ? res3.getReservationId() : "N/A") + " ---");
        if (res3 != null && res3.getStatus().equals("CONFIRMED")) { // Check status before completing
            rentalService.completeReservation(res3.getReservationId());
        }

        System.out.println("\n--- Demo End ---");
    }
}
