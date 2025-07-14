import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class MovieTicketBookingSystem {

    /* ---------- ENUMS ---------- */
    enum SeatType { NORMAL, PREMIUM }

    enum BookingStatus { ACTIVE, CANCELLED }

    enum PaymentStatus { PENDING, COMPLETED, FAILED }

    /* ---------- MODELS ---------- */
    static class User {
        final String userId;
        final String name;

        public User(String userId, String name) {
            this.userId = userId;
            this.name = name;
        }
    }

    static class Movie {
        final String movieId;
        final String title;

        public Movie(String movieId, String title) {
            this.movieId = movieId;
            this.title = title;
        }
    }

    static class Seat {
        final String seatId;
        final SeatType type;
        private boolean booked;

        public Seat(String seatId, SeatType type) {
            this.seatId = seatId;
            this.type = type;
            this.booked = false;
        }

        public synchronized boolean book() {
            if (!booked) {
                booked = true;
                return true;
            }
            return false;
        }

        public synchronized void unbook() {
            booked = false;
        }

        public synchronized boolean isBooked() {
            return booked;
        }

        public String getSeatId() {
            return seatId;
        }

        public SeatType getType() {
            return type;
        }
    }

    static class Show {
        final String showId;
        final Movie movie;
        final Date startTime;
        final Map<String, Seat> seats;

        public Show(String showId, Movie movie, Date startTime, List<Seat> seatList) {
            this.showId = showId;
            this.movie = movie;
            this.startTime = startTime;
            this.seats = new ConcurrentHashMap<>();
            for (Seat seat : seatList) {
                seats.put(seat.getSeatId(), seat);
            }
        }

        public Seat getSeat(String seatId) {
            return seats.get(seatId);
        }
    }

    static class Screen {
        final String screenId;
        final List<Show> shows;

        public Screen(String screenId) {
            this.screenId = screenId;
            this.shows = new ArrayList<>();
        }

        public void addShow(Show show) {
            shows.add(show);
        }
    }

    static class Theater {
        final String theaterId;
        final String name;
        final List<Screen> screens;

        public Theater(String theaterId, String name) {
            this.theaterId = theaterId;
            this.name = name;
            this.screens = new ArrayList<>();
        }

        public void addScreen(Screen screen) {
            screens.add(screen);
        }
    }

    static class Payment {
        final String paymentId;
        final double amount;
        PaymentStatus status;

        public Payment(String paymentId, double amount) {
            this.paymentId = paymentId;
            this.amount = amount;
            this.status = PaymentStatus.PENDING;
        }

        public void complete() {
            status = PaymentStatus.COMPLETED;
        }
    }

    static class Booking {
        final String bookingId;
        final User user;
        final Show show;
        final List<Seat> seats;
        final Payment payment;
        BookingStatus status;

        public Booking(String bookingId, User user, Show show, List<Seat> seats, Payment payment) {
            this.bookingId = bookingId;
            this.user = user;
            this.show = show;
            this.seats = seats;
            this.payment = payment;
            this.status = BookingStatus.ACTIVE;
        }

        public void cancel() {
            if (status == BookingStatus.ACTIVE) {
                status = BookingStatus.CANCELLED;
                for (Seat seat : seats) {
                    seat.unbook();
                }
            }
        }
    }

    /* ---------- PRICING STRATEGY ---------- */
    interface PricingStrategy {
        double calculatePrice(Seat seat);
    }

    static class DefaultPricingStrategy implements PricingStrategy {
        @Override
        public double calculatePrice(Seat seat) {
            return seat.getType() == SeatType.NORMAL ? 200.0 : 400.0;
        }
    }

    /* ---------- SEAT KEY (COMPOSITE) ---------- */
    static class SeatKey {
        final String showId;
        final String seatId;

        public SeatKey(String showId, String seatId) {
            this.showId = showId;
            this.seatId = seatId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SeatKey)) return false;
            SeatKey seatKey = (SeatKey) o;
            return Objects.equals(showId, seatKey.showId) &&
                   Objects.equals(seatId, seatKey.seatId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(showId, seatId);
        }

        @Override
        public String toString() {
            return showId + "::" + seatId;
        }
    }

    /* ---------- SEAT LOCK SYSTEM ---------- */
    static class SeatLock {
        final SeatKey key;
        final String userId;
        final long expiryTime;

        public SeatLock(SeatKey key, String userId, long durationMs) {
            this.key = key;
            this.userId = userId;
            this.expiryTime = System.currentTimeMillis() + durationMs;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }

    static class SeatLockManager {
        private final Map<SeatKey, SeatLock> locks = new ConcurrentHashMap<>();
        private final Map<SeatKey, ReentrantLock> seatLocks = new ConcurrentHashMap<>();

        public void lockSeatForUser(String showId, String seatId, String userId, long durationMs) throws Exception {
            SeatKey key = new SeatKey(showId, seatId);
            seatLocks.computeIfAbsent(key, k -> new ReentrantLock()).lock();
            try {
                if (locks.containsKey(key) && !locks.get(key).isExpired()) {
                    throw new Exception("Seat already locked: " + seatId + " in show " + showId);
                }
                locks.put(key, new SeatLock(key, userId, durationMs));
            } finally {
                seatLocks.get(key).unlock();
            }
        }

        public boolean isSeatLockedByUser(String showId, String seatId, String userId) {
            SeatKey key = new SeatKey(showId, seatId);
            SeatLock lock = locks.get(key);
            return lock != null && !lock.isExpired() && lock.userId.equals(userId);
        }

        public void releaseSeat(String showId, String seatId, String userId) {
            SeatKey key = new SeatKey(showId, seatId);
            SeatLock lock = locks.get(key);
            if (lock != null && lock.userId.equals(userId)) {
                locks.remove(key);
            }
        }

        public void releaseAllSeatsForUser(String userId) {
            locks.entrySet().removeIf(e -> e.getValue().userId.equals(userId));
        }
    }

    /* ---------- BOOKING SERVICE ---------- */
    static class BookingService {
        private final SeatLockManager seatLockManager;
        private final PricingStrategy pricingStrategy;
        private final Map<String, Booking> bookings = new ConcurrentHashMap<>();
        private static final long SEAT_LOCK_DURATION_MS = 2 * 60 * 1000; // 2 minutes

        public BookingService(SeatLockManager seatLockManager, PricingStrategy pricingStrategy) {
            this.seatLockManager = seatLockManager;
            this.pricingStrategy = pricingStrategy;
        }

        public void holdSeats(User user, Show show, List<String> seatIds) throws Exception {
            for (String seatId : seatIds) {
                seatLockManager.lockSeatForUser(show.showId, seatId, user.userId, SEAT_LOCK_DURATION_MS);
            }
        }

        public Booking confirmBooking(User user, Show show, List<String> seatIds) throws Exception {
            double totalPrice = 0;
            List<Seat> bookedSeats = new ArrayList<>();

            // Check locks
            for (String seatId : seatIds) {
                if (!seatLockManager.isSeatLockedByUser(show.showId, seatId, user.userId)) {
                    throw new Exception("Seat not properly locked by user: " + seatId);
                }
            }

            // Actually book seats
            for (String seatId : seatIds) {
                Seat seat = show.getSeat(seatId);
                if (seat == null || seat.isBooked()) {
                    throw new Exception("Seat unavailable: " + seatId);
                }
                if (!seat.book()) {
                    throw new Exception("Failed to book seat: " + seatId);
                }
                totalPrice += pricingStrategy.calculatePrice(seat);
                bookedSeats.add(seat);
            }

            // Simulate payment
            Payment payment = new Payment(UUID.randomUUID().toString(), totalPrice);
            payment.complete();

            Booking booking = new Booking(UUID.randomUUID().toString(), user, show, bookedSeats, payment);
            bookings.put(booking.bookingId, booking);

            // Release locks
            seatLockManager.releaseAllSeatsForUser(user.userId);

            return booking;
        }

        public void cancelBooking(String bookingId) throws Exception {
            Booking booking = bookings.get(bookingId);
            if (booking == null) throw new Exception("Booking not found");
            booking.cancel();
        }
    }

    /* ---------- ADMIN SERVICE ---------- */
    static class AdminService {
        private final List<Movie> movies = new ArrayList<>();
        private final List<Theater> theaters = new ArrayList<>();

        public void addMovie(Movie movie) { movies.add(movie); }
        public List<Movie> listMovies() { return movies; }

        public void addTheater(Theater theater) { theaters.add(theater); }
        public List<Theater> listTheaters() { return theaters; }
    }

    /* ---------- MAIN DEMO ---------- */
    public static void main(String[] args) throws Exception {
        // Setup services
        SeatLockManager lockManager = new SeatLockManager();
        PricingStrategy pricingStrategy = new DefaultPricingStrategy();
        BookingService bookingService = new BookingService(lockManager, pricingStrategy);
        AdminService adminService = new AdminService();

        // Admin adds movie and theater
        Movie movie = new Movie("M1", "Interstellar");
        adminService.addMovie(movie);

        Theater theater = new Theater("T1", "IMAX");
        Screen screen = new Screen("S1");

        List<Seat> seats = Arrays.asList(
                new Seat("A1", SeatType.NORMAL),
                new Seat("A2", SeatType.PREMIUM)
        );

        Show show = new Show("SH1", movie, new Date(), seats);
        screen.addShow(show);
        theater.addScreen(screen);
        adminService.addTheater(theater);

        // User
        User user = new User("U1", "Alice");

        // Booking flow
        System.out.println("--- Holding seats ---");
        bookingService.holdSeats(user, show, Arrays.asList("A1", "A2"));
        System.out.println("Seats held for user.");

        System.out.println("--- Confirming booking ---");
        Booking booking = bookingService.confirmBooking(user, show, Arrays.asList("A1", "A2"));
        System.out.println("Booking ID: " + booking.bookingId);

        System.out.println("--- Cancelling booking ---");
        bookingService.cancelBooking(booking.bookingId);
        System.out.println("Booking cancelled, seats available again.");
    }
}
