This code models an elevator controller that decides how to move one elevator in a 10 story building.
It models users requesting an elevator using the UP and DOWN buttons on each floor, entering the elevator when the elevator reaches the floor on which the request initiated, and then pressing a button on the elevator to indicate their destination floor.


Assumptions: 
On floor 1, users can only press up.  On floor 10, users can only press down.
Inside the elevator, each user only presses one button
At a given time, the elevator can be heading up, heading down, or staying idle (if no floors had the U/D button pushed and no elevator buttons are pushed)
Requests will be evenly distributed among the floors

Decisions made:
1.  Do we want to prioritize minimizing the elevator’s distance travelled or do we want to minimize the chance that a user waits too long for the elevator to arrive?  
  Eg:  If at time 0, a user enters on floor 2 and presses the elevator button to go to floor 10, and a user on floor 1 presses up, should the elevator start going down to floor 1 or continue for floor 10 before returning to floor 1?

    **Always check for requests in the same direction the elevator is travelling. We want to prevent starvation.** Eg: Let the elevator continue to floor 10, because otherwise we may run into the situation where the elevator never reaches 10 because there’s always a request from a floor closer to the current location of the elevator. 
3.  If the elevator is currently not moving, and multiple users press different floor request buttons, how do we decide which floor to go to first?

    **We’ll go towards the first floor button pressed**
5.  If the elevator is currently not moving and no floor buttons are pressed, but multiple elevator buttons are pressed, which direction do we travel?

    **We’ll go towards the closest floor to the current floor.**