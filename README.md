# Robotic Factory Simulator

- `simulator`: the factory model, Swing viewer, TP1 simulation threads, and TP2 persistence server/client.
- `simulation-service`: the TP3 Spring Boot service that runs simulations remotely.
- TP4: the service publishes factory snapshots through Kafka, and the desktop
  viewer consumes them to refresh the drawing.

The Git tag `tp2-complete` preserves the completed TP2 version.

## Import into Eclipse

Requirements: a JDK supporting Java 17 and Eclipse IDE for Java Developers with
Maven integration (m2e). The projects target Java 17. Configure the installed JDK
under **Preferences > Java > Installed JREs**, and assign a compatible JDK to the
**JavaSE-17** execution environment if Eclipse asks for one.

1. Clone or extract the complete repository.
2. In Eclipse, select **File > Import > General > Existing Projects into Workspace**.
3. Select the repository folder as the root directory and import both projects:
   - `fr.tp.inf112.projects.robotsim` (`simulator`)
   - `fr.tp.slr201.projects.robotsim.service.simulation` (`simulation-service`)
4. Keep the checked-in project names. The service's Java Build Path references the
   simulator project by its Eclipse name, as in the TP3 handout.
5. Allow Maven to download the service's dependencies. If needed, right-click the
   service and select **Maven > Update Project**, then **Project > Clean**.

Build and run both projects together in Eclipse. The simulator remains a plain
Java project; its runtime libraries, including Jackson, are included under
`simulator/libs` and use project-relative build paths. The service uses Maven for
its Spring dependencies. Internet access is needed for the first Maven resolution.
Spring Tools is optional: the service can run as a standard Java application.

## Run the distributed simulator

First start ZooKeeper and Kafka from a Kafka `3.8.1` installation, following the
TP4 handout. The simulation service expects the broker at `localhost:9092`.
Kafka is installed separately from this Git repository. In two terminals opened
in the Kafka installation folder, run:

```sh
bin/zookeeper-server-start.sh config/zookeeper.properties
```

```sh
bin/kafka-server-start.sh config/server.properties
```

Wait for ZooKeeper to start before starting Kafka. The broker's
`config/server.properties` should advertise `localhost:9092` for local clients.

Right-click each shared `.launch` file and select **Run As > Java Application**,
starting them in this order:

| Launch file | Application | Default port |
| --- | --- | --- |
| `simulator/config/PersistenceServer.launch` | TP2 factory storage server | `5050` |
| `simulation-service/config/SimulationService.launch` | Spring simulation service | `8080` |
| `simulator/config/SimulatorApplication.launch` | Desktop factory viewer | — |

The launch configurations use Eclipse workspace variables rather than absolute
paths. The persistence server stores `.factory` files in the `simulator` project
folder. Saved factories and generated logs are ignored by Git.

In the viewer, save the initial factory with a `.factory` filename, or open an
existing saved factory, before starting the animation. The Spring service loads
the saved factory by its ID. The viewer uses HTTP to start and stop the simulation,
and a background Kafka consumer receives JSON snapshots to update the drawing on
Swing's UI thread. Stop the animation before terminating the applications in Eclipse.

The service also publishes factory snapshots, including the running and stopped
states, to a per-factory Kafka topic named `simulation-<factory-id>` (for example,
`simulation-example.factory`). The viewer subscribes to that same topic. Each
Kafka poll waits up to 100 ms for messages; it does not request snapshots from
the HTTP service.

The HTTP endpoints are:

- `GET http://localhost:8080/simulation/start?id=example.factory`
- `GET http://localhost:8080/simulation?id=example.factory`
- `GET http://localhost:8080/simulation/stop?id=example.factory`

The snapshot endpoint remains available for TP3 compatibility and debugging.
The TP4 viewer uses the start and stop endpoints for control, and Kafka for model
updates.

If Start has no effect, check that the Spring console reports that Tomcat started
on port `8080`. ZooKeeper, Kafka, the persistence server and the viewer cannot
replace the simulation service. If Spring reports a logging conflict, check that
the simulator build path contains only the bundled SLF4J `2.0.18` API, rather than
the older SLF4J API from the Kafka distribution.

The default setup runs all three applications on the same computer. The TP2
persistence clients also accept the JVM properties `robotsim.server.host` and
`robotsim.server.port`; the viewer currently calls the HTTP service on
`localhost:8080`.

## Run the JSON checks

In the simulator project, right-click `TestRobotSimSerializationJSON.java` and
select **Run As > JUnit Test**. Both tests should pass. Eclipse supplies the
project's JUnit 5 library; the servers do not need to be running for these tests.

The initial commit history was organized into exercise steps after implementation.
