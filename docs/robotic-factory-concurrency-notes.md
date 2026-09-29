**Robotic Factory — Exercise 1: code changes and explanations**

These notes connect the [exercise PDF](<01 Robotic Factory Concurrent Programming.pdf>) to the simulator code in [the Downloads project](..).

The exercise converts a sequential simulation into one where each component runs in its own thread. It also asks robots to escape mutual blockage and coordinate access to positions.

When these notes were first written, the project had no Git history. A reconstructed history has since been added; see the repository README. The before/after comparisons below use saved Eclipse versions. Those versions are intermediate snapshots, so they do not prove every difference from the original starter project. Java snippets preserve the inspected logic, with whitespace cleaned up. No Java source was changed when making these notes.

| Exercise task | PDF page | Relevant code |
| --- | --- | --- |
| Give both robots the same first destinations | 6 | `SimulatorApplication.main()` |
| Identify which robot occupies a position | 6–7 | `Factory.getMobileComponentAt()` |
| Detect and escape mutual blockage | 6–7 | `Robot.isLivelyLocked()`, `findFreeNeighbouringPosition()`, `moveToNextPathPosition()` |
| Give each component a repeating task | 8 | `Component implements Runnable`, `Component.run()` |
| Start one thread per component | 8 | `Factory.startSimulation()`, `Factory.behave()` |
| Protect occupancy checks and movement | 8 | `synchronized (getFactory())` in `Robot` |

1. **Reorder Robot 2's destinations**

   Source: [SimulatorApplication.java:68](../src/fr/tp/inf112/projects/robotsim/app/SimulatorApplication.java:68).

   The earlier saved version sent Robot 2 to the charging station first. Its room has a closed door, so Robot 2 could not reach it and stopped immediately. The change moves that destination to the end:

   ```diff
   -robot2.addTargetComponent(chargingStation);
    robot2.addTargetComponent(machine1);
    robot2.addTargetComponent(machine2);
    robot2.addTargetComponent(conveyor1);
   +robot2.addTargetComponent(chargingStation);
   ```

   Robot 1 already used that order in the baseline selected for this comparison. Both robots now follow:

   ```text
   Machine 1 → Machine 2 → Conveyor 1 → Charging Station
   ```

   This allows both robots to move and encounter each other. It does not open the charging-room door: the charging station remains unreachable later.

2. **Find the robot occupying a position**

   Source: [Factory.java:167](../src/fr/tp/inf112/projects/robotsim/model/Factory.java:167).

   The exercise asks for a lookup that returns the blocking component. The existing `hasMobileComponentAt()` only returns `true` or `false`.

   These two overloads are present in the current implementation:

   ```java
   public Component getMobileComponentAt(final Position position,
                                        final Component ignoredComponent) {
       if (position == null) {
           return null;
       }

       return getMobileComponentAt(
           new RectangularShape(position.getxCoordinate(),
                                position.getyCoordinate(), 2, 2),
           ignoredComponent);
   }

   public Component getMobileComponentAt(final PositionedShape shape,
                                        final Component ignoredComponent) {
       if (shape == null) {
           return null;
       }

       for (final Component component : getComponents()) {
           if (component != ignoredComponent
                   && component.isMobile()
                   && component.overlays(shape)) {
               return component;
           }
       }

       return null;
   }
   ```

   The first overload converts a position into a small rectangular area. The second searches for a mobile component overlapping that area. `ignoredComponent` prevents a robot from finding itself; `null` means no matching component was found.

   **History detail:** these methods already appear in the earliest saved `Factory` version used here. They support the exercise, but their addition cannot be shown by comparing that snapshot with the current file.

3. **Detect and escape a livelock**

   A robot already remembered an unsuccessful movement target in `blockedTargetPosition`. The new logic recognizes when two robots are waiting for each other's positions:

   ```text
   Robot A occupies X and wants Y.
   Robot B occupies Y and wants X.

   Both keep checking; neither makes progress.
   ```

   **Added method: `isLivelyLocked()`**

   Source: [Robot.java:230](../src/fr/tp/inf112/projects/robotsim/model/Robot.java:230).

   ```java
   public boolean isLivelyLocked() {
       final Position blockedTargetPosition = getBlockedTargetPosition();

       if (blockedTargetPosition == null) {
           return false;
       }

       final Component otherRobot =
           getFactory().getMobileComponentAt(blockedTargetPosition, this);

       return otherRobot != null
           && getPosition().equals(((Robot) otherRobot).getBlockedTargetPosition());
   }
   ```

   First, check whether this robot has a blocked target. Then find the robot occupying that target. Finally, check whether the other robot's blocked target equals this robot's current position. `Position.equals()` compares the coordinates.

   The cast to `Robot` assumes the mobile component found is a robot. This detector handles the two-robot situation described in the exercise; it does not detect every possible cycle involving more robots.

   **Added method: `findFreeNeighbouringPosition()`**

   Source: [Robot.java:155](../src/fr/tp/inf112/projects/robotsim/model/Robot.java:155).

   ```java
   private Position findFreeNeighbouringPosition() {
       final Position currentPosition = getPosition();
       final int step = getFactory().getPathResolution();

       final int x = currentPosition.getxCoordinate();
       final int y = currentPosition.getyCoordinate();

       final Position[] neighbours = {
           new Position(x + step, y),
           new Position(x, y + step),
           new Position(x, y - step),
           new Position(x - step, y)
       };

       for (final Position position : neighbours) {
           final PositionedShape shape = new RectangularShape(
               position.getxCoordinate(),
               position.getyCoordinate(),
               2,
               2
           );

           if (!getFactory().hasObstacleAt(shape)
                   && !getFactory().hasMobileComponentAt(shape, this)) {
               return position;
           }
       }

       return null;
   }
   ```

   This checks four neighbouring positions, in the order shown, and returns the first one without an obstacle or another robot. The step comes from the factory's path resolution, which is `5` in this application's setup. If every candidate is blocked, the method returns `null`.

   The `2 × 2` rectangle is the actual check used in this version. An intermediate Eclipse snapshot used `this.getWidth()` and `this.getHeight()` instead; the current implementation hardcodes `2` for both dimensions, matching `computeMotion()`.

   **Changed method: `moveToNextPathPosition()`**

   Source: [Robot.java:121](../src/fr/tp/inf112/projects/robotsim/model/Robot.java:121).

   Before, the method only attempted movement and notified observers:

   ```java
   private int moveToNextPathPosition() {
       final Motion motion = computeMotion();

       final int displacement = motion == null ? 0 : motion.moveToTarget();

       notifyObservers();

       return displacement;
   }
   ```

   The current implementation includes both livelock recovery and the synchronization explained in item 6:

   ```java
   private int moveToNextPathPosition() {
       int displacement;

       synchronized (getFactory()) {
           final Motion motion = computeMotion();
           displacement = motion == null ? 0 : motion.moveToTarget();
       }

       if (displacement != 0) {
           notifyObservers();
       } else if (isLivelyLocked()) {
           final Position freeNeighbouringPosition = findFreeNeighbouringPosition();

           if (freeNeighbouringPosition != null) {
               blockedTargetPosition = freeNeighbouringPosition;
               displacement = moveToNextPathPosition();
               computePathToCurrentTargetComponent();
           }
       }

       return displacement;
   }
   ```

   `displacement` is the distance moved; zero means no movement. It is no longer `final` because the recovery attempt can assign it a new value.

   When a mutual blockage is detected, the robot stores a free neighbour as its temporary target. The recursive call attempts that sidestep, and then the robot recomputes its path towards the same destination component. Existing target-selection code gives `blockedTargetPosition` priority, which is why assigning that field redirects the next attempt.

   Observer notification now happens when displacement is nonzero. It is outside the synchronized block, so the movement lock is released before notifying the view.

4. **Give each component its own repeating task**

   Source: [Component.java:10](../src/fr/tp/inf112/projects/robotsim/model/Component.java:10) and [Component.java:140](../src/fr/tp/inf112/projects/robotsim/model/Component.java:140).

   The current declaration includes `Runnable`:

   ```java
   public abstract class Component implements Figure, Serializable, Runnable {
   ```

   The exercise requires this interface. The available saved `Component` version already included it, but did not yet contain the following added method:

   ```java
   @Override
   public void run() {
       while (isSimulationStarted()) {
           behave();

           try {
               Thread.sleep(50);
           } catch (final InterruptedException ex) {
               System.err.println("Component thread was interrupted");
           }
       }
   }
   ```

   `Runnable` supplies the task that a thread executes. Each component repeatedly performs one behaviour step and sleeps for 50 milliseconds while the simulation is running. Sleeping pauses that component's thread, not all component threads.

   `Robot` inherits `run()` and overrides `behave()`. Therefore, a robot's thread executes the robot's movement behaviour. Components without an overridden behaviour still get threads, but their default `behave()` returns `false`.

   The interval is approximately 50 milliseconds plus the time spent in `behave()` and scheduling delays. It is not an exact timing guarantee.

5. **Replace the factory's sequential loop with thread startup**

   Source: [Factory.java:112](../src/fr/tp/inf112/projects/robotsim/model/Factory.java:112).

   **Change to `startSimulation()`**

   Previously, the factory repeatedly called `behave()` and slept for 100 milliseconds. Now it calls `behave()` once; repetition is handled by `Component.run()`.

   ```diff
    public void startSimulation() {
        if (!isSimulationStarted()) {
            this.simulationStarted = true;
            notifyObservers();

   -        while (isSimulationStarted()) {
   -            behave();
   -
   -            try {
   -                Thread.sleep(100);
   -            }
   -            catch (final InterruptedException ex) {
   -                System.err.println("Simulation was abruptly interrupted");
   -            }
   -        }
   +        behave();
        }
    }
   ```

   **Change to `Factory.behave()`**

   ```diff
    @Override
    public boolean behave() {
        boolean behaved = true;

        for (final Component component : getComponents()) {
   -        behaved = component.behave() || behaved;
   +        final Thread thread = new Thread(component);
   +        thread.start();
        }

        return behaved;
    }
   ```

   Previously, the factory called each component's `behave()` directly, one after another. Now each `new Thread(component)` receives that component as its `Runnable`, and `start()` launches its `run()` method on a separate thread.

   Calling `run()` directly would execute on the calling thread. Calling `start()` is what creates the separate execution.

   Removing the factory's outer loop is necessary: leaving it in place with the new `behave()` would repeatedly create more threads for the same components.

6. **Make the occupancy check and movement one protected operation**

   The synchronization added to `Robot.moveToNextPathPosition()` surrounds both operations:

   ```java
   synchronized (getFactory()) {
       final Motion motion = computeMotion();
       displacement = motion == null ? 0 : motion.moveToTarget();
   }
   ```

   `computeMotion()` checks whether another mobile component occupies the target. If the position is occupied, it memorizes the blocked target and returns `null`. Otherwise, it returns a `Motion` object. `moveToTarget()` then updates the robot's coordinates.

   Without synchronization, this interleaving is possible:

   ```text
   Robot A: checks position P → free
   Robot B: checks position P → free
   Robot A: moves to P
   Robot B: moves to P
   ```

   This is a race condition: two individually valid checks produce an invalid combined result.

   With the shared factory lock:

   ```text
   Robot A: acquires the factory lock
   Robot A: checks P, then moves to P
   Robot A: releases the factory lock
   Robot B: acquires the same lock
   Robot B: checks P → occupied, so it does not move there
   ```

   Both robots refer to the same factory object, so they compete for the same lock. `synchronized (this)` inside `Robot` would use a different lock for each robot and would not coordinate them.

   Both the check and the update must stay inside the block. Locking only the check would still let another robot move between checking and updating. Components run concurrently, but their protected movement operations execute one at a time.

   This uses the same mutual-exclusion idea as `synchronized` methods in the commands-buffer exercise. There are no `wait()` or `notifyAll()` calls in this robot implementation: a robot blocked by occupancy normally retries during a later behaviour step, with special handling for mutual blockage.

The resulting execution flow is:

```text
Start Animation
  → SimulatorController.startAnimation()
  → Factory.startSimulation()
  → Set simulationStarted = true
  → Factory.behave() starts one thread per component
  → Each component thread executes Component.run()
      → Call its own behave()
      → For a robot, attempt movement under the shared factory lock
      → If mutually blocked, try a sidestep and recalculate the path
      → Sleep for 50 ms and repeat while the simulation is running

Stop Animation
  → Factory.stopSimulation() sets simulationStarted = false
  → Component loops are intended to exit when they observe that change
```

**Notes about the current implementation**

- The source currently declares `private transient boolean simulationStarted;`. `transient` concerns serialization, not thread visibility. Declaring it `volatile`, or consistently synchronizing access, would make updates reliably visible to the component threads. This is a suggested improvement, not a change already made.
- The shared lock protects the occupancy check and coordinate update. Livelock detection and neighbour selection happen outside that block, so they are not a single synchronized snapshot of all robot state. Finding a neighbour does not reserve it; the recursive movement attempt checks occupancy again under the lock.
- The catch block in `Component.run()` prints a message and continues. Interrupting a component thread is not currently implemented as a request to terminate it.
- These notes are based on source inspection and Eclipse history comparisons. The simulator was not launched or tested while writing them.

**Comparison sources**

The earlier versions used above are relative to this workspace's `fr.tp.inf112.projects.robotsim/.metadata/.plugins/org.eclipse.core.resources/.history/` directory:

| File | Saved version used for comparison |
| --- | --- |
| `SimulatorApplication.java` | `5b/907c54818db5001117e8f2803fd6c5ab` |
| `Component.java` | `d2/803b2f0e9fb5001119d5d6243a7fe84b` |
| `Factory.java` | `73/505b6a429fb5001119d5d6243a7fe84b` |
| `Robot.java` | `a0/30a166969cb5001117e8f2803fd6c5ab` |
| Intermediate `Robot.java`, using robot dimensions for neighbour checks | `f5/809e66da9cb5001117e8f2803fd6c5ab` |
