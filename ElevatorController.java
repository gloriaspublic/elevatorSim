import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

public class ElevatorController {

    private static final int NUM_FLOORS = 10;
    private static final int BOTTOM_FLOOR = 1;

    private enum Direction {
        UP,
        DOWN,
        IDLE
    }

    /*
     * This class models the request made by a passanger pressing
     *  a floor's Up or Down button.  
     */
    private static class Request implements Comparable<Request> {
        int floor; // The floor where the request originated (e.g., where the Up or Down button was pressed)
        Direction direction; //Was the Up or Down button pressed
        int destination; // The floor the passenger wants to go to
        int time; // Time the request was made

        public Request(int floor, Direction direction, int destination,int time) {
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



    private int currentFloor = BOTTOM_FLOOR;
    private Direction elevatorDirection = Direction.IDLE;
    private PriorityBlockingQueue<Request> floorRequestsQueue = new PriorityBlockingQueue<>();
    //Set will ensure we don't store duplicates of buttons pressed
    private Set<Integer> setOfElevatorButtonsPressed = new HashSet<>();
    //This is just used for testing
    private ArrayList<Integer> floorsVisited = new ArrayList<>();
    private int currentTime = 0;

    public void handleFloorButtonPress(Request myRequest) {
        floorRequestsQueue.add(myRequest);
        System.out.println("Event: Floor button pressed. floor: " + myRequest.floor + ", direction: " + myRequest.direction + ", destination:" + myRequest.destination);
    }

    public void handleElevatorButtonPress(int destination) {
        setOfElevatorButtonsPressed.add(destination);
        System.out.println("Event: Elevator button pressed for floor: " + destination );
    }

    private boolean atLeastOneRequestRemains(){
        if(!floorRequestsQueue.isEmpty() || !setOfElevatorButtonsPressed.isEmpty()){
            return true;
        }
        return false;
    }

    /*
     * This method determines the next direction the elevator should move in.
     * The elevator will continue moving in the current direction if there are more requests in that direction.
     * If there are no more requests in the current direction, but requests in the opposite direction, the elevator will change direction.
     * If there are no requests, the elevator will remain idle.
     * If the elevator is currently idle, it will move towards the first floor request, if there 
     * are no floor requests, it will move towards the closest elevator button pressed.
     */
    private void updateElevatorDirection() {
        //If at least one elevator button or floor request button is pressed, we handle the edge case of being at the TOP or BOTTOM floor
        if (atLeastOneRequestRemains()) {
            if (currentFloor == NUM_FLOORS) {
                elevatorDirection = Direction.DOWN;
                return;
            } else if (currentFloor == BOTTOM_FLOOR) {
                elevatorDirection = Direction.UP;
                return;
            }
        }

        //If the elevator is moving up, and there are any requests above the current floor, keep the direction as up.
        //If we were going up, but there are no more requests above the current floor, change the direction to idle.
        if(elevatorDirection == Direction.UP){  
            for(Request request : floorRequestsQueue){
                if(request.floor > currentFloor){
                    return;
                }
            }
            for(Integer floor : setOfElevatorButtonsPressed){
                if(floor > currentFloor){
                    return;
                }
            }
            elevatorDirection = Direction.IDLE;
        }

        //If the elevator is moving down, and there are any requests below the current floor, keep the direction as down.
        //If we were going down, but there are no more requests below the current floor, change the direction to idle.
        if(elevatorDirection == Direction.DOWN){ 
            for(Request request : floorRequestsQueue){
                if(request.floor < currentFloor){
                    return;
                }
            }
            for(Integer floor : setOfElevatorButtonsPressed){
                if(floor < currentFloor){
                    return;
                }
            }
            elevatorDirection = Direction.IDLE;
        }

        //If the elevator is idle, and there are more floor requests, set the direction based on whether the floor request is above or below the current floor.
        if (elevatorDirection == Direction.IDLE) {
            if (!floorRequestsQueue.isEmpty()) {
                Request nextRequest = floorRequestsQueue.peek();
                if (nextRequest.floor > currentFloor) {
                    elevatorDirection = Direction.UP;
                } else {
                    elevatorDirection = Direction.DOWN;
                }
            }
            // If no external requests, go to the closest elevator button pressed
            else if (!setOfElevatorButtonsPressed.isEmpty()) {
                Integer nextStop = getNearestStop();
                if (nextStop != null) {
                    if (nextStop > currentFloor) {
                        elevatorDirection = Direction.UP;
                    } else {
                        elevatorDirection = Direction.DOWN;
                    }
                } else {
                    //If there are no more elevator buttons pressed, or floor requests, the elevator remains idle.
                    elevatorDirection = Direction.IDLE;
                }
            }
        }

    }

    private ArrayList<Integer> getFloorsVisited() {
        return floorsVisited;
    }

    private void initializeVariables(int initialFloor) {
        currentFloor = initialFloor;
        elevatorDirection = Direction.IDLE;
        floorRequestsQueue.clear();
        floorsVisited.clear();
        setOfElevatorButtonsPressed.clear();
        currentTime = 0;
    }

    /*
     * This method returns the nearest stop in the direction of the elevator
     * based on the current floor and the set of elevator buttons pressed.
     */
    private  Integer getNearestStop() {
        //If there are no elevator buttons pressed, return the lobby floor
        if (setOfElevatorButtonsPressed.isEmpty()) {
            return BOTTOM_FLOOR;
        }
        return Collections.min(setOfElevatorButtonsPressed,
                Comparator.comparingInt(f -> Math.abs(f - currentFloor)));
    }

    private void moveElevator() {
        if (elevatorDirection == Direction.UP && currentFloor < NUM_FLOORS) {
            currentFloor++;
        } else if (elevatorDirection == Direction.DOWN && currentFloor > BOTTOM_FLOOR) {
            currentFloor--;
        }
    }

    private void letPassengersExit(){
        if (setOfElevatorButtonsPressed.contains(currentFloor)) {
            System.out.println("Action: Passenger(s) exiting elevator on floor: " + currentFloor );
            setOfElevatorButtonsPressed.remove(currentFloor);
        }
    }

    private void letPassengersEnter(){
        Iterator<Request> iterator = floorRequestsQueue.iterator();
        while (iterator.hasNext()) {
            Request request = iterator.next();
            if (request.floor == currentFloor) {
                System.out.println("Action: Passenger(s) entering elevator on floor: " + currentFloor);
                handleElevatorButtonPress(request.destination);
                iterator.remove(); 
            }
        }
    }

    private void processRequests() {

        //Track the order of floors visited for testing
        floorsVisited.add(currentFloor);

        // Check if any passengers need to get off at the current floor
        letPassengersExit();

        //Check if we are at a requested floor. if so, then model passengers entering the elevator and pressing a button
        //Then remove this floor request from the queue.
        letPassengersEnter();

        //Determine the elevator's next direction
       updateElevatorDirection();
       
       System.out.println("Status: Time: " + currentTime + ", floor: " + currentFloor + ", direction: " + elevatorDirection);

        // If the elevator is moving towards a request, continue in that direction
        if (elevatorDirection != Direction.IDLE) {
            moveElevator();
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
     * Expected sequence of floors visited: 3 -> 10 -> 2.
     */
    public static void testCase0() {

        System.out.println("START TEST CASE 0:");
        ElevatorController controller = new ElevatorController();
        controller.initializeVariables(1);
        Map<Integer, List<Request>> mapOfTimesToRequest = new HashMap<>();
        mapOfTimesToRequest.put(0, Arrays.asList(
                new Request(1, Direction.UP, 10, 0),
                new Request(1, Direction.UP, 3, 0)));
        mapOfTimesToRequest.put(1,
                Arrays.asList(new Request(3, Direction.DOWN, 2, 1)));
        ArrayList<Integer> expectedFloorsVisited = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 9, 8, 7, 6, 5, 4, 3, 2));
        controller.runSimulation(mapOfTimesToRequest, expectedFloorsVisited.size());
        assert controller.getFloorsVisited().equals(expectedFloorsVisited);
        System.out.println("END TEST CASE 0:");

    }

    /*
     * Test Case 1: Elevator starts on floor 10.
     * Time 0: 3 users on Floor 10 hit Down to go to floors 1, 3, and 4.
     * Time 1: user on Floor 3 hits Up to go to floor 4.
     * Expected sequence of floors visited: 4 -> 3 -> 1 -> 4.
     */
    public static void testCase1() {
        System.out.println("START TEST CASE 1:");
        ElevatorController controller = new ElevatorController();
        controller.initializeVariables(10);
        Map<Integer, List<Request>> mapOfTimesToRequest = new HashMap<>();
        mapOfTimesToRequest.put(0, Arrays.asList(
                new Request(10, Direction.DOWN, 1, 0),
                new Request(10, Direction.DOWN, 3, 0),
                new Request(10, Direction.DOWN, 4, 0)));
        mapOfTimesToRequest.put(1,
                Arrays.asList(new Request(3, Direction.UP, 4, 1)));

        ArrayList<Integer> expectedFloorsVisited = new ArrayList<>(Arrays.asList(10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 2, 3, 4));
        controller.runSimulation(mapOfTimesToRequest, expectedFloorsVisited.size());
        assert controller.getFloorsVisited().equals(expectedFloorsVisited);

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
        ElevatorController controller = new ElevatorController();
        controller.initializeVariables(5);
        Map<Integer, List<Request>> mapOfTimesToRequest = new HashMap<>();
        mapOfTimesToRequest.put(0, Arrays.asList(
                new Request(8, Direction.DOWN, 2, 0),
                new Request(7, Direction.UP, 10, 0),
                new Request(3, Direction.UP, 4, 0),
                new Request(3, Direction.DOWN, 2, 0)));

        ArrayList<Integer> expectedFloorsVisited = new ArrayList<>(Arrays.asList(5, 6, 7, 8, 9, 10, 9, 8, 7, 6, 5, 4, 3, 2, 3, 4));
        controller.runSimulation(mapOfTimesToRequest, expectedFloorsVisited.size());
        assert controller.getFloorsVisited().equals(expectedFloorsVisited);
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
        ElevatorController controller = new ElevatorController(); 
        controller.initializeVariables(1);
        Map<Integer, List<Request>> mapOfTimesToRequest = new HashMap<>();
        mapOfTimesToRequest.put(0, Arrays.asList(
                new Request(3, Direction.DOWN, 2, 0)));
        mapOfTimesToRequest.put(1, Arrays.asList(
                new Request(10, Direction.DOWN, 1, 1)));

        ArrayList<Integer> expectedFloorsVisited = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1));
        controller.runSimulation(mapOfTimesToRequest, expectedFloorsVisited.size());
        assert controller.getFloorsVisited().equals(expectedFloorsVisited);
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
        ElevatorController controller = new ElevatorController(); 
        controller.initializeVariables(1);
        Map<Integer, List<Request>> mapOfTimesToRequest = new HashMap<>();
        mapOfTimesToRequest.put(0, Arrays.asList(
                new Request(5, Direction.UP, 8, 0),
                new Request(3, Direction.DOWN, 2, 0),
                new Request(1, Direction.UP, 3, 0)));
        mapOfTimesToRequest.put(1, Arrays.asList(
                new Request(2, Direction.UP, 4, 1)));

        ArrayList<Integer> expectedFloorsVisited = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 7, 6, 5, 4, 3, 2));
        controller.runSimulation(mapOfTimesToRequest, expectedFloorsVisited.size());
        assert controller.getFloorsVisited().equals(expectedFloorsVisited);
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