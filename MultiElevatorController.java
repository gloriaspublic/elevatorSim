import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

public class MultiElevatorController {

    private static final int NUM_FLOORS = 10;
    private static final int BOTTOM_FLOOR = 1;
    private static final int NUM_ELEVATORS = 2;

    private enum Direction {
        UP,
        DOWN,
        IDLE
    }

    /*
     * This class models the request made by a passanger pressing
     * a floor's Up or Down button.
     */
    private static class Request implements Comparable<Request> {
        int floor; // The floor where the request originated (e.g., where the Up or Down button was
                   // pressed)
        Direction direction; // Was the Up or Down button pressed
        int destination; // The floor the passenger wants to go to
        int time; // Time the request was made

        public Request(int floor, Direction direction, int destination, int time) {
            this.floor = floor;
            this.direction = direction;
            this.destination = destination;
            this.time = time;
        }

        @Override
        public int compareTo(Request other) {
            return Integer.compare(this.time, other.time);
        }

        @Override
        public String toString() {
            return "Request{" +
                    "floor=" + floor +
                    ", direction=" + direction +
                    ", destination=" + destination +
                    ", time=" + time +
                    '}';
        }
    }

    public static class Elevator {
        private int elevatorID;
        private int currentFloor;
        private Direction direction;
        // Set will ensure we don't store duplicates of buttons pressed
        private Set<Integer> elevatorButtonsPressed;
        private PriorityBlockingQueue<Request> el_floorRequestsQueue;
        // floorsVisited is only used for testing
        private ArrayList<Integer> floorsVisited = new ArrayList<>();

        public Elevator(int id) {
            this.elevatorID = id;
            this.currentFloor = BOTTOM_FLOOR;
            this.direction = Direction.IDLE;
            this.elevatorButtonsPressed = new HashSet<>();
            this.el_floorRequestsQueue = new PriorityBlockingQueue<>();
        }

        public int numOfTotalRequests() {
            return el_floorRequestsQueue.size() + elevatorButtonsPressed.size();
        }

        public void addRequest(Request request) {
            el_floorRequestsQueue.add(request);
            System.out.println("Event: Elevator: " + elevatorID + " was assigned request: " + request.floor
                    + request.direction + " with destination: " + request.destination);
            if (direction == Direction.IDLE) {
                updateElevatorDirection();
            }
        }

        public int getCurrentFloor() {
            return currentFloor;
        }

        public Direction getDirection() {
            return direction;
        }

        public void handleElevatorButtonPress(int destination) {
            elevatorButtonsPressed.add(destination);
            System.out.println("Event: Elevator: " + elevatorID + " button pressed for floor: " + destination);
        }

        private boolean atLeastOneRequestRemains() {
            if (!el_floorRequestsQueue.isEmpty() || !elevatorButtonsPressed.isEmpty()) {
                return true;
            }
            return false;
        }

        private ArrayList<Integer> getFloorsVisited() {
            return floorsVisited;
        }

        private void processElevatorRequests(int currentTime) {
            // Track the order of floors visited for testing
            floorsVisited.add(currentFloor);

            // Check if any passengers need to get off at the current floor
            letPassengersExit();

            // Check if we are at a requested floor. if so, then model passengers entering
            // the elevator and pressing a button
            // Then remove this floor request from the queue.
            letPassengersEnter();

            // Determine the elevator's next direction
            updateElevatorDirection();

            System.out.println(
                    "Status: Time: " + currentTime + ", ElevatorID: " + elevatorID + ", floor: " + currentFloor
                            + ", direction: " + direction);

            // If the elevator is moving towards a request, continue in that direction
            if (direction != Direction.IDLE) {
                move();
            }
        }

        private void move() {
            if (direction == Direction.UP && currentFloor < NUM_FLOORS) {
                currentFloor++;
            } else if (direction == Direction.DOWN && currentFloor > BOTTOM_FLOOR) {
                currentFloor--;
            }
        }

        private void letPassengersExit() {
            if (elevatorButtonsPressed.contains(currentFloor)) {
                System.out.println(
                        "Action: Elevator: " + elevatorID + " Passenger(s) exiting elevator on floor: " + currentFloor);
                elevatorButtonsPressed.remove(currentFloor);
            }
        }

        private void letPassengersEnter() {
            Iterator<Request> iterator = el_floorRequestsQueue.iterator();
            while (iterator.hasNext()) {
                Request request = iterator.next();
                if (request.floor == currentFloor) {
                    System.out.println("Action: Elevator: " + elevatorID + " Passenger(s) entering elevator on floor: "
                            + currentFloor);
                    handleElevatorButtonPress(request.destination);
                    iterator.remove();
                }
            }
        }

        /*
         * This method determines the next direction the elevator should move in.
         * The elevator will continue moving in the current direction if there are more
         * requests in that direction.
         * If there are no more requests in the current direction, but requests in the
         * opposite direction, the elevator will change direction.
         * If there are no requests, the elevator will remain idle.
         * If the elevator is currently idle, it will move towards the first floor
         * request, if there
         * are no floor requests, it will move towards the closest elevator button
         * pressed.
         */
        private void updateElevatorDirection() {
            // If at least one elevator button or floor request button is pressed, we handle
            // the edge case of being at the TOP or BOTTOM floor
            if (atLeastOneRequestRemains()) {
                if (currentFloor == NUM_FLOORS) {
                    direction = Direction.DOWN;
                    return;
                } else if (currentFloor == BOTTOM_FLOOR) {
                    direction = Direction.UP;
                    return;
                }
            }

            // If the elevator is moving up, and there are any requests above the current
            // floor, keep the direction as up.
            // If the elevator is moving up, and there are no more requests above the
            // current floor,
            // change the direction to idle.
            if (direction == Direction.UP) {
                for (Request request : el_floorRequestsQueue) {
                    if (request.floor > currentFloor) {
                        return;
                    }
                }
                for (Integer floor : elevatorButtonsPressed) {
                    if (floor > currentFloor) {
                        return;
                    }
                }
                direction = Direction.IDLE;
            }

            // If the elevator is moving down, and there are any requests below the current
            // floor, keep the direction as down.
            // If the elevator is moving down, and there are no more requests below the
            // current
            // floor, change the direction to idle.
            if (direction == Direction.DOWN) {
                for (Request request : el_floorRequestsQueue) {
                    if (request.floor < currentFloor) {
                        return;
                    }
                }
                for (Integer floor : elevatorButtonsPressed) {
                    if (floor < currentFloor) {
                        return;
                    }
                }
                direction = Direction.IDLE;
            }

            // If the elevator is idle, and there are more floor requests, set the direction
            // based on whether the floor request is above or below the current floor.
            if (direction == Direction.IDLE) {
                if (!el_floorRequestsQueue.isEmpty()) {
                    Request nextRequest = el_floorRequestsQueue.peek();
                    if (nextRequest.floor > currentFloor) {
                        direction = Direction.UP;
                    } else {
                        direction = Direction.DOWN;
                    }
                }
                // If no external requests, go to the closest elevator button pressed
                else if (!elevatorButtonsPressed.isEmpty()) {
                    Integer nextStop = getNearestStop();
                    if (nextStop != null) {
                        if (nextStop > currentFloor) {
                            direction = Direction.UP;
                        } else {
                            direction = Direction.DOWN;
                        }
                    }
                } else {
                    // If there are no more elevator buttons pressed, or floor requests, the
                    // elevator remains idle.
                    direction = Direction.IDLE;
                }
            }

        }

        /*
         * This method returns the nearest stop in the direction of the elevator
         * based on the current floor and the set of elevator buttons pressed.
         */
        private Integer getNearestStop() {
            // If there are no elevator buttons pressed, return the lobby floor
            if (elevatorButtonsPressed.isEmpty()) {
                return BOTTOM_FLOOR;
            }
            return Collections.min(elevatorButtonsPressed,
                    Comparator.comparingInt(f -> Math.abs(f - currentFloor)));
        }

    }

    private PriorityBlockingQueue<Request> floorRequestsQueue = new PriorityBlockingQueue<>();
    private int currentTime = 0;
    private ArrayList<Elevator> elevators = new ArrayList<>();

    public MultiElevatorController() {
        for (int i = 0; i < NUM_ELEVATORS; i++) {
            elevators.add(new Elevator(i));
        }
    }

    public void handleFloorButtonPress(Request myRequest) {
        floorRequestsQueue.add(myRequest);
        System.out.println("Event: Floor button pressed. floor: " + myRequest.floor + ", direction: "
                + myRequest.direction + ", destination:" + myRequest.destination);
    }

    private void initializeVariables(int initialFloor) {
        for (Elevator elevator : elevators) {
            elevator.currentFloor = initialFloor;
            elevator.direction = Direction.IDLE;
            elevator.el_floorRequestsQueue.clear();
            elevator.elevatorButtonsPressed.clear();
            elevator.floorsVisited.clear();
        }
        floorRequestsQueue.clear();
        currentTime = 0;
    }

    public Elevator getBestElevator(Request request) {
        // If the elevator already has a request for the same floor in the same
        // direction, return that elevator
        // Otherwise, return the elevator that has the least number of total requests
        // and at is either IDLE or going in the same direction as the request
        Elevator bestElevator = null;
        int minRequests = Integer.MAX_VALUE;

        for (Elevator elevator : elevators) {
            // If the request's direction is up and the elevator is going up
            // with the cuurent floor is less than the request floor OR the elevator is idle
            if (request.direction == Direction.UP
                    && ((elevator.getDirection() == request.direction
                            && elevator.getCurrentFloor() <= request.floor)
                            || elevator.getDirection() == Direction.IDLE)) {
                int totalRequests = elevator.numOfTotalRequests();
                if (totalRequests < minRequests) {
                    minRequests = totalRequests;
                    bestElevator = elevator;
                }
            }
            // If the request's direction is down and the elevator is either idle or going
            // down and the cuurent floor is above the requested floor
            else if (request.direction == Direction.DOWN
                    && ((elevator.getDirection() == request.direction
                            && elevator.getCurrentFloor() >= request.floor)
                            || elevator.getDirection() == Direction.IDLE)) {
                int totalRequests = elevator.numOfTotalRequests();
                if (totalRequests < minRequests) {
                    minRequests = totalRequests;
                    bestElevator = elevator;
                }
            }
            // loop through the requests for this elevator
            for (Request r : elevator.el_floorRequestsQueue) {
                // if this elev has a request for the same floor in the same direction, we will
                // use this elevator
                if (r.floor == request.floor && r.direction == request.direction) {
                    return elevator;
                }
            }
        }
        return bestElevator;
    }

    private void processRequests() {

        // Loop through all the floor requests and assign them to the elevator with the
        // least number of requests
        Iterator<Request> iterator = floorRequestsQueue.iterator();
        while (iterator.hasNext()) {
            Request request = iterator.next();
            Elevator bestElevator = getBestElevator(request);
            if (bestElevator != null) {
                bestElevator.addRequest(request);
                iterator.remove();
            }
        }

        // Loop through all the elevators and process their requests
        for (Elevator elevator : elevators) {
            elevator.processElevatorRequests(currentTime);
        }
    }

    public void runSimulation(Map<Integer, List<Request>> mapOfTimesToRequest, int totalSimTime) {
        for (int i = 0; i < totalSimTime; i++) {
            List<Request> requests = mapOfTimesToRequest.get(currentTime);
            if (requests != null) {
                for (Request r : requests) {
                    handleFloorButtonPress(r);
                }
            }
            processRequests();
            currentTime++;
        }
    }

    /*
     * Test Case 0: Elevator starts on floor 1.
     * Time 0: 2 users on Floor 1 hit UP to go to floors 10 and 3.
     * Time 1: user on Floor 3 hits DOWN to go to floor 2.
     * Expected result: Elevator 1 goes to floors 1, 2, 3, 4, 5, 6, 7, 8, 9, 10
     * and Elevator 2 goes to floors 1, 2, 3, 2
     */
    public static void testCase0() {

        System.out.println("START TEST CASE 0:");
        MultiElevatorController controller = new MultiElevatorController();
        controller.initializeVariables(1);
        Map<Integer, List<Request>> mapOfTimesToRequest = new HashMap<>();
        mapOfTimesToRequest.put(0, Arrays.asList(
                new Request(1, Direction.UP, 10, 0),
                new Request(1, Direction.UP, 3, 0)));
        mapOfTimesToRequest.put(1,
                Arrays.asList(new Request(3, Direction.DOWN, 2, 1)));
        ArrayList<Integer> expectedFloorsVisitedE1 = new ArrayList<>(
                Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        ArrayList<Integer> expectedFloorsVisitedE2 = new ArrayList<>(
                Arrays.asList(1, 1, 2, 3, 2, 2, 2, 2, 2, 2));
        controller.runSimulation(mapOfTimesToRequest, expectedFloorsVisitedE1.size());
        ArrayList<Integer> floorsVisitedE1 = controller.elevators.get(0).getFloorsVisited();
        ArrayList<Integer> floorsVisitedE2 = controller.elevators.get(1).getFloorsVisited();
        assert floorsVisitedE1.equals(expectedFloorsVisitedE1);
        assert floorsVisitedE2.equals(expectedFloorsVisitedE2);
        System.out.println("END TEST CASE 0:");

    }

    /*
     * Test Case 1: Elevator starts on floor 10.
     * Time 0: 3 users on Floor 10 hit Down to go to floors 1, 3, and 4.
     * Time 1: user on Floor 3 hits Up to go to floor 4.
     * Expected result: Elevator 1 goes to floors 1, 2, 3, 4, 5, 6, 7, 8, 9, 10
     * and Elevator 2 goes to floors 1, 2, 3, 2
     */
    public static void testCase1() {
        System.out.println("START TEST CASE 1:");
        MultiElevatorController controller = new MultiElevatorController();
        controller.initializeVariables(10);
        Map<Integer, List<Request>> mapOfTimesToRequest = new HashMap<>();
        mapOfTimesToRequest.put(0, Arrays.asList(
                new Request(10, Direction.DOWN, 1, 0),
                new Request(10, Direction.DOWN, 3, 0),
                new Request(10, Direction.DOWN, 4, 0)));
        mapOfTimesToRequest.put(1,
                Arrays.asList(new Request(3, Direction.UP, 4, 1)));

        ArrayList<Integer> expectedFloorsVisitedE1 = new ArrayList<>(
                Arrays.asList(10, 9, 8, 7, 6, 5, 4, 3, 2, 1));
        ArrayList<Integer> expectedFloorsVisitedE2 = new ArrayList<>(
                Arrays.asList(10, 10, 9, 8, 7, 6, 5, 4, 3, 4));
        controller.runSimulation(mapOfTimesToRequest, expectedFloorsVisitedE1.size());
        ArrayList<Integer> floorsVisitedE1 = controller.elevators.get(0).getFloorsVisited();
        ArrayList<Integer> floorsVisitedE2 = controller.elevators.get(1).getFloorsVisited();
        assert floorsVisitedE1.equals(expectedFloorsVisitedE1);
        assert floorsVisitedE2.equals(expectedFloorsVisitedE2);

        System.out.println("END TEST CASE 1");
    }

    /*
     * Test Case 2: Elevator starts on floor 5,
     * Time 0: User on Floor 8 presses Down to go to 2, user on Floor 7 presses UP
     * to go to 10
     * Time 0: User on Floor 3 presses UP to go to 4 and User on Floor 3 presses
     * DOWN to go to 2.
     * Expected sequence of floors visited: 7 -> 8 -> 10 -> 3 -> 2 -> 4.
     */
    public static void testCase2() {
        System.out.println("START TEST CASE 2:");
        MultiElevatorController controller = new MultiElevatorController();
        controller.initializeVariables(5);
        Map<Integer, List<Request>> mapOfTimesToRequest = new HashMap<>();
        mapOfTimesToRequest.put(0, Arrays.asList(
                new Request(8, Direction.DOWN, 2, 0),
                new Request(7, Direction.UP, 10, 0),
                new Request(3, Direction.UP, 4, 0),
                new Request(3, Direction.DOWN, 2, 0)));

        ArrayList<Integer> expectedFloorsVisitedE1 = new ArrayList<>(
                Arrays.asList(5, 6, 7, 8, 7, 6, 5, 4, 3, 2, 2, 2));
        ArrayList<Integer> expectedFloorsVisitedE2 = new ArrayList<>(
                Arrays.asList(5, 6, 7, 8, 9, 10, 10, 9, 8, 7, 6, 5));
        controller.runSimulation(mapOfTimesToRequest, expectedFloorsVisitedE1.size());
        ArrayList<Integer> floorsVisitedE1 = controller.elevators.get(0).getFloorsVisited();
        ArrayList<Integer> floorsVisitedE2 = controller.elevators.get(1).getFloorsVisited();
        assert floorsVisitedE1.equals(expectedFloorsVisitedE1);
        assert floorsVisitedE2.equals(expectedFloorsVisitedE2);
        System.out.println("END TEST CASE 2");
    }

    /*
     * Test Case 3: Elevator starts on floor 1,
     * Time 0: User on Floor 3 presses Down to go to 2
     * Time 1: User on Floor 10 presses Down to go to 1.
     * Elevator should go to floor 3, then 10, then 2, then 1.
     */
    public static void testCase3() {
        System.out.println("START TEST CASE 3:");
        MultiElevatorController controller = new MultiElevatorController();
        controller.initializeVariables(1);
        Map<Integer, List<Request>> mapOfTimesToRequest = new HashMap<>();
        mapOfTimesToRequest.put(0, Arrays.asList(
                new Request(3, Direction.DOWN, 2, 0)));
        mapOfTimesToRequest.put(1, Arrays.asList(
                new Request(10, Direction.DOWN, 1, 1)));

        ArrayList<Integer> expectedFloorsVisitedE1 = new ArrayList<>(
                Arrays.asList(1, 2, 3, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2));
        ArrayList<Integer> expectedFloorsVisitedE2 = new ArrayList<>(
                Arrays.asList(1, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1));
        controller.runSimulation(mapOfTimesToRequest, expectedFloorsVisitedE2.size());
        ArrayList<Integer> floorsVisitedE1 = controller.elevators.get(0).getFloorsVisited();
        ArrayList<Integer> floorsVisitedE2 = controller.elevators.get(1).getFloorsVisited();
        assert floorsVisitedE1.equals(expectedFloorsVisitedE1);
        assert floorsVisitedE2.equals(expectedFloorsVisitedE2);
        System.out.println("END TEST CASE 3");
    }

    /*
     * Test Case 4: Elevator starts on floor 1,
     * Time 0: User on Floor 5 presses Up to go to 8
     * Time 0: User on Floor 3 presses Down to go to 2
     * Time 0: User on Floor 1 presses Up to go to 3
     * Time 1: User on Floor 2 presses Up to to 4
     * Elevator should go to floor 1, 2, 3, 4, 5, 8, 2
     */
    public static void testCase4() {
        System.out.println("START TEST CASE 4:");
        MultiElevatorController controller = new MultiElevatorController();
        controller.initializeVariables(1);
        Map<Integer, List<Request>> mapOfTimesToRequest = new HashMap<>();
        mapOfTimesToRequest.put(0, Arrays.asList(
                new Request(5, Direction.UP, 8, 0),
                new Request(3, Direction.DOWN, 2, 0),
                new Request(1, Direction.UP, 3, 0)));
        mapOfTimesToRequest.put(1, Arrays.asList(
                new Request(2, Direction.UP, 4, 1)));
        ArrayList<Integer> expectedFloorsVisitedE1 = new ArrayList<>(
                Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8));
        ArrayList<Integer> expectedFloorsVisitedE2 = new ArrayList<>(
                Arrays.asList(1, 2, 3, 4, 3, 2, 2, 2));
        controller.runSimulation(mapOfTimesToRequest, expectedFloorsVisitedE1.size());
        ArrayList<Integer> floorsVisitedE1 = controller.elevators.get(0).getFloorsVisited();
        ArrayList<Integer> floorsVisitedE2 = controller.elevators.get(1).getFloorsVisited();
        assert floorsVisitedE1.equals(expectedFloorsVisitedE1);
        assert floorsVisitedE2.equals(expectedFloorsVisitedE2);
        System.out.println("END TEST CASE 4");
    }

    public static void main(String[] args) {
        testCase0();
        testCase1();
        testCase2();
        testCase3();
        testCase4();
    }
}