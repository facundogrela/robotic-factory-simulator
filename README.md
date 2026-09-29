# Robotic Factory Simulator

Telecom Paris advanced Java programming exercises.

This repository's history was reconstructed on 2026-09-29 from the supplied
starter ZIP, saved practice files, and the completed project. Commits group the
work by learning step; they do not represent the original dates of development.
Intermediate versions that combine these sources are reconstructed snapshots.

The initial simulator source is imported from robotic-factory-simulator.zip.
Runtime JARs, their accompanying licenses, and Eclipse project settings are
included. Generated files and unused parts of the dependency distribution are
excluded. The optional JGraphT source attachment is not bundled.


## Run in Eclipse

1. Install a JDK supporting Java 17 or later.
2. Import this folder using File > Import > General > Existing Projects into Workspace.
3. Open Run > Run Configurations > Java Application and run `PersistenceServer`.
   Its shared configuration is `config/PersistenceServer.launch`.
4. Run `SimulatorApplication` using `config/SimulatorApplication.launch`.
5. Use File > Save Canvas to enter a name, then File > Open Canvas to load it.

The default server is `localhost:5050`. Models are stored in the server's working
folder, which the supplied launch sets to this project folder. Saved `.factory`
files, logs, and Eclipse build output are ignored by Git.

For separate computers, set `-Drobotsim.server.host=<server address>` on the
simulator. To change the port, set `-Drobotsim.server.port=<port>` on both JVMs.

## Exercise 2 implementation and verification

Logging, the Hello World practice, the persistence server, the remote persistence
manager, the remote chooser, and application wiring are implemented. The Hello
World text protocol is preserved in its dedicated historical commit; the final
application uses Java object streams.

All reconstructed code stages compile with `javac --release 17`. The final Java
source matches the already tested project byte for byte. Earlier socket checks
passed for saving, loading, updating, listing files, reporting server errors,
and keeping files in the server directory. The Swing dialogs were not tested
interactively by the assistant.

Before submitting, run the server and simulator, save a factory, reopen it, and
check the console and `config/robotism*.log`. Complete the PDF's CONFIG/FINE
logging experiments. The logger and handler thresholds both affect which
messages appear. The handlers use different formatters: `XMLFormatter` for the
file and `SimpleFormatter` for the console. The current file size limit is
50,000 bytes with one rotating file; older records may be replaced during a
verbose run. Remote `delete()` is intentionally unimplemented, as permitted by
the assignment.

## Learning notes

- [Concurrency exercise](docs/robotic-factory-concurrency-notes.md)
- [Remote persistence and Save/Open flow](docs/robotic-factory-remote-persistence-notes.md)
- [Exercise 1 PDF](<docs/01 Robotic Factory Concurrent Programming.pdf>)
- [Exercise 2 PDF](<docs/02 Robotic Factory Data Persistence Client-Server Architecture.pdf>)

## Sharing with the professor

This repository is intended to be private. Invite the professor through the
repository's Settings > Collaborators, then send them the repository link.
Alternatively, use Code > Download ZIP and submit that archive through the
course platform. Inviting a collaborator is a separate step from creating the
repository.
