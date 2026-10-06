# Robotic Factory Simulator

- `simulator`: TP1 and the completed TP2 persistence simulator.
- `simulation-service`: TP3 Spring Boot project and the current start controller.

The Git tag `tp2-complete` preserves the completed TP2 version.

In Eclipse, import both folders as existing projects. Run
`simulator/config/PersistenceServer.launch`, then
`simulator/config/SimulatorApplication.launch` for TP2.
The persistence server uses port 5050 and saves `.factory` files in the simulator folder.

For TP3, run the service's `Application` class. HTTP mappings, retrieval, and stop
operations are still to be implemented.

Open this repository folder in VS Code. The Eclipse-based Java extension uses
both projects' checked-in Eclipse build paths.

The initial commit history was organized into exercise steps after implementation.
