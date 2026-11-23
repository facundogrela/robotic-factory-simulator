# Robotic Factory Simulator

SLR201 project: parallel simulation (TP1), client-server persistence (TP2),
Spring Boot service (TP3) and Kafka events (TP4).

- `simulator`: factory model, Swing viewer and persistence server.
- `simulation-service`: simulation service.

## Setup

Requires Java 17 or later, Eclipse with Maven (m2e), and Kafka 3.8.1.
The simulator libraries are included in `simulator/libs`.

In Eclipse, use **File > Import > Existing Projects into Workspace** to import
both projects. Keep their names: the service references the simulator project.
Configure a JDK compatible with JavaSE-17 and let Maven download the service
dependencies. If needed, use **Maven > Update Project**.

## Run

From the Kafka installation folder, run these commands in separate terminals.
Wait for ZooKeeper to start before starting Kafka.

```sh
bin/zookeeper-server-start.sh config/zookeeper.properties
```

```sh
bin/kafka-server-start.sh config/server.properties
```

The broker must be accessible at `localhost:9092`.
Then run the Eclipse launch configurations in this order:

1. `simulator/config/PersistenceServer.launch` (port 5050).
2. `simulation-service/config/SimulationService.launch` (port 8080).
3. `simulator/config/SimulatorApplication.launch`.

Save the factory with a `.factory` filename, or open an existing factory,
before starting the animation. Files are stored in the `simulator` folder.
Stop the animation before closing the applications.

The service provides GET endpoints `/simulation/start?id=...`,
`/simulation?id=...` and `/simulation/stop?id=...`. The viewer uses HTTP to
start and stop the simulation, and Kafka topic `simulation-<id>` for updates.

## JSON test

In Eclipse, run `TestRobotSimSerializationJSON` with **Run As > JUnit Test**
to check model serialization, as described in TP3.
