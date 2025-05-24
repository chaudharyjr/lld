import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

// --- Enums ---
enum Direction { UP, DOWN, IDLE }
enum ElevatorState { MOVING, STOPPED_DOORS_CLOSED, STOPPED_DOORS_OPEN, IDLE }
enum RequestType { EXTERNAL, INTERNAL }

// --- Elevator Request ---
class ElevatorRequest {
    private static final AtomicInteger ID_COUNTER = new AtomicInteger();
    private final int id;
    private final int floor;
    private final Direction direction; // For external requests
    private final RequestType type;
    private final long timestamp;

    // External request
    public ElevatorRequest(int floor, Direction direction) {
        this.id = ID_COUNTER.incrementAndGet();
        this.floor = floor;
        this.direction = direction;
        this.type = RequestType.EXTERNAL;
        this.timestamp = System.currentTimeMillis();
    }
    // Internal request
    public ElevatorRequest(int floor) {
        this.id = ID_COUNTER.incrementAndGet();
        this.floor = floor;
        this.direction = Direction.IDLE;
        this.type = RequestType.INTERNAL;
        this.timestamp = System.currentTimeMillis();
    }
    public int getFloor() { return floor; }
    public Direction getDirection() { return direction; }
    public RequestType getType() { return type; }
    public long getTimestamp() { return timestamp; }
}

// --- Elevator Movement Strategy ---
interface ElevatorMovementStrategy {
    int determineNextStop(int currentFloor, Direction currentDirection,
                          Set<Integer> internalDestinations, Set<Integer> externalPickups,
                          int minFloor, int maxFloor);
}

class ScanLookMovementStrategy implements ElevatorMovementStrategy {
    @Override
    public int determineNextStop(int currentFloor, Direction currentDirection,
                                 Set<Integer> internalDestinations, Set<Integer> externalPickups,
                                 int minFloor, int maxFloor) {
        Set<Integer> allDestinations = new HashSet<>(internalDestinations);
        allDestinations.addAll(externalPickups);
        if (allDestinations.isEmpty()) return -1;
        if (currentDirection == Direction.UP || currentDirection == Direction.IDLE) {
            Optional<Integer> nextUp = allDestinations.stream().filter(f -> f >= currentFloor).min(Integer::compare);
            if (nextUp.isPresent()) return nextUp.get();
        }
        if (currentDirection == Direction.DOWN || currentDirection == Direction.IDLE) {
            Optional<Integer> nextDown = allDestinations.stream().filter(f -> f <= currentFloor).max(Integer::compare);
            if (nextDown.isPresent()) return nextDown.get();
        }
        if (currentDirection == Direction.UP) {
            return allDestinations.stream().filter(f -> f < currentFloor).max(Integer::compare).orElse(-1);
        } else if (currentDirection == Direction.DOWN) {
            return allDestinations.stream().filter(f -> f > currentFloor).min(Integer::compare).orElse(-1);
        }
        return allDestinations.stream().min(Comparator.comparingInt(f -> Math.abs(f - currentFloor))).orElse(-1);
    }
}

// --- Elevator ---
class Elevator implements Runnable {
    private final int id;
    private int currentFloor;
    private ElevatorState state;
    private Direction direction;
    private final int minFloor;
    private final int maxFloor;
    private ElevatorMovementStrategy movementStrategy;
    private final Set<Integer> internalDestinations = ConcurrentHashMap.newKeySet();
    private final Set<Integer> externalPickups = ConcurrentHashMap.newKeySet();
    private final int maxCapacity;
    private int currentLoad = 0; // Just a counter for capacity

    public Elevator(int id, int minFloor, int maxFloor, ElevatorMovementStrategy strategy, int maxCapacity) {
        this.id = id;
        this.minFloor = minFloor;
        this.maxFloor = maxFloor;
        this.currentFloor = minFloor;
        this.state = ElevatorState.IDLE;
        this.direction = Direction.IDLE;
        this.movementStrategy = strategy;
        this.maxCapacity = maxCapacity;
        System.out.printf("[Elevator %d] Initialized at F%d%n", id, currentFloor);
    }
    public int getId() { return id; }
    public int getCurrentFloor() { return currentFloor; }
    public void addExternalPickup(int floor) { externalPickups.add(floor); }
    public void addInternalDestination(int floor) { internalDestinations.add(floor); }
    public ElevatorState getState() { return state; }
    public Direction getDirection() { return direction; }

    @Override
    public void run() {
        while (true) {
            try {
                processMovement();
                Thread.sleep(800);
            } catch (InterruptedException e) {
                System.out.printf("[Elevator %d] Interrupted.%n", id);
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void processMovement() throws InterruptedException {
        int nextStop = movementStrategy.determineNextStop(currentFloor, direction, internalDestinations, externalPickups, minFloor, maxFloor);
        if (nextStop == -1) {
            if (state != ElevatorState.IDLE) {
                state = ElevatorState.IDLE;
                direction = Direction.IDLE;
                System.out.printf("[Elevator %d] IDLE at F%d.%n", id, currentFloor);
            }
            return;
        }
        if (currentFloor == nextStop) {
            stopAndHandleFloor();
        } else {
            if (nextStop > currentFloor) {
                direction = Direction.UP;
                state = ElevatorState.MOVING;
                currentFloor++;
            } else if (nextStop < currentFloor) {
                direction = Direction.DOWN;
                state = ElevatorState.MOVING;
                currentFloor--;
            }
            System.out.printf("[Elevator %d] Moving %s. Floor: %d. Target: %d.%n", id, direction, currentFloor, nextStop);
            if (internalDestinations.contains(currentFloor) || externalPickups.contains(currentFloor)) {
                stopAndHandleFloor();
            }
        }
    }

    private void stopAndHandleFloor() throws InterruptedException {
        state = ElevatorState.STOPPED_DOORS_OPEN;
        System.out.printf("[Elevator %d] Arrived at F%d. Doors opening.%n", id, currentFloor);
        Thread.sleep(1000);

        // Simulate people getting off (internal requests)
        if (internalDestinations.contains(currentFloor)) {
            System.out.printf("[Elevator %d] Passengers exit at F%d.%n", id, currentFloor);
            internalDestinations.remove(currentFloor);
            currentLoad = Math.max(0, currentLoad - 1); // For simulation, one leaves
        }
        // Simulate people getting on (external requests)
        if (externalPickups.contains(currentFloor)) {
            externalPickups.remove(currentFloor);
            // For demo, simulate a random number of people entering and pressing random floors
            int entering = Math.min(maxCapacity - currentLoad, 1 + new Random().nextInt(2));
            for (int i = 0; i < entering; i++) {
                int dest;
                do {
                    dest = minFloor + new Random().nextInt(maxFloor - minFloor + 1);
                } while (dest == currentFloor);
                addInternalDestination(dest);
                System.out.printf("[Elevator %d] Passenger entered at F%d, pressed F%d.%n", id, currentFloor, dest);
            }
            currentLoad += entering;
        }
        state = ElevatorState.STOPPED_DOORS_CLOSED;
        Thread.sleep(500);
    }
}

// --- Elevator Selection Strategy ---
interface ElevatorSelectionStrategy {
    Elevator selectElevator(ElevatorRequest req, List<Elevator> elevators);
}
class NearestCarSelectionStrategy implements ElevatorSelectionStrategy {
    @Override
    public Elevator selectElevator(ElevatorRequest req, List<Elevator> elevators) {
        // Choose elevator with minimal distance and not busy
        return elevators.stream()
                .min(Comparator.comparingInt(e -> Math.abs(e.getCurrentFloor() - req.getFloor())))
                .orElse(elevators.get(0));
    }
}

// --- Panel Controller ---
class PanelController {
    private ElevatorSelectionStrategy selectionStrategy;
    public PanelController(ElevatorSelectionStrategy selectionStrategy) {
        this.selectionStrategy = selectionStrategy;
    }
    public void setStrategy(ElevatorSelectionStrategy s) { this.selectionStrategy = s; }
    public Elevator assignElevator(ElevatorRequest req, List<Elevator> elevators) {
        return selectionStrategy.selectElevator(req, elevators);
    }
}

// --- Dispatcher ---
class Dispatcher {
    private final PanelController panelController;
    private final Building building;

    public Dispatcher(PanelController panelController, Building building) {
        this.panelController = panelController;
        this.building = building;
    }
    public void receiveRequest(ElevatorRequest req) {
        Elevator elevator = panelController.assignElevator(req, building.getElevators());
        if (req.getType() == RequestType.EXTERNAL) {
            elevator.addExternalPickup(req.getFloor());
        } else {
            elevator.addInternalDestination(req.getFloor());
        }
    }
}

// --- Building ---
class Building {
    private final List<Elevator> elevators = new ArrayList<>();
    private final int minFloor, maxFloor;
    public Building(int minFloor, int maxFloor) {
        this.minFloor = minFloor;
        this.maxFloor = maxFloor;
    }
    public void addElevator(Elevator e) { elevators.add(e); }
    public List<Elevator> getElevators() { return elevators; }
    public int getMinFloor() { return minFloor; }
    public int getMaxFloor() { return maxFloor; }
}

// --- Main Simulation ---
public class ElevatorLLD {
    public static void main(String[] args) throws InterruptedException {
        int minFloor = 1, maxFloor = 10, elevatorCount = 3, maxCapacity = 5;
        Building building = new Building(minFloor, maxFloor);
        PanelController panelController = new PanelController(new NearestCarSelectionStrategy());
        Dispatcher dispatcher = new Dispatcher(panelController, building);

        // Add elevators
        for (int i = 1; i <= elevatorCount; i++) {
            Elevator elevator = new Elevator(i, minFloor, maxFloor, new ScanLookMovementStrategy(), maxCapacity);
            building.addElevator(elevator);
            new Thread(elevator, "Elevator-" + i).start();
        }

        // Simulate external requests (users pressing up/down on floors)
        Random rand = new Random();
        for (int i = 0; i < 20; i++) {
            int floor = rand.nextInt(maxFloor - minFloor + 1) + minFloor;
            Direction dir = (floor == maxFloor) ? Direction.DOWN : (floor == minFloor) ? Direction.UP : (rand.nextBoolean() ? Direction.UP : Direction.DOWN);
            dispatcher.receiveRequest(new ElevatorRequest(floor, dir));
            Thread.sleep(1200);
        }
    }
}
