**Robotic Factory — completed remote persistence walkthrough**

The remote persistence flow is connected to `SimulatorApplication`. This document contains snapshots of the installed code plus explanations.

**Run it in Eclipse**

1. Refresh the simulator project (`fr.tp.inf112.projects.robotsim` in Downloads).
2. Stop any older server still using port 5050.
3. Run the new `config/PersistenceServer.launch` using Run As → Java Application, or select PersistenceServer under Run Configurations → Java Application.
4. Run the existing SimulatorApplication configuration.
5. Use File → Save Canvas and enter a name such as `my-factory`. The file is saved as `my-factory.factory` in the server's working folder.
6. Use File → Open Canvas. Choose a filename from the server's dropdown list.

The supplied server launch uses the simulator project directory as its working folder. On this computer that is `the simulator project folder`. It is normal for saved files to appear there rather than in `config`. Refresh the project to see files created by the server.

Saving an already named model reuses its filename. The viewer asks for a save name when the model does not have an ID yet.

**What the parts mean**

- Chooser: asks which filename to use.
- Remote manager: sends the requested save/load operation to the server.
- Server: decides what operation was requested.
- Original persistence manager: reads and writes the actual file on the server.

**1. Choose a filename: RemoteFileCanvasChooser**

The inherited methods are entry points used by the viewer:

- `choseCanvas()` calls `browseCanvases(true)` when opening.
- `newCanvasId()` calls `browseCanvases(false)` when choosing a new save name, then adds the configured extension if needed.

Java uses the overridden `browseCanvases()` in our subclass even when the caller is an inherited method. Our override forwards to methods with clearer names.

`chooseSaveFileName()` opens a text field. Cancel returns `null`. Invalid names show a message and repeat the prompt. It returns a filename, not a saved file. The inherited code then adds `.factory` when necessary.

`chooseFileToOpen()` asks the server for filenames. `String[]` is an array of names. An empty array produces an information dialog. For a nonempty array, `showInputDialog()` receives the array as `selectionValues`, making a dropdown. Choosing a name returns a `String`; cancel returns `null`.

The constructors call `super(...)` to initialize the parent class. `getViewer()` supplies the parent window for the dialogs. `canvasChooser.setViewer(factoryViewer)` connects that window after its construction.

Source: [RemoteFileCanvasChooser.java](../src/fr/tp/inf112/projects/robotsim/app/RemoteFileCanvasChooser.java)

```java
package fr.tp.inf112.projects.robotsim.app;

import java.awt.Component;
import java.io.IOException;

import javax.swing.JOptionPane;

import fr.tp.inf112.projects.canvas.view.FileCanvasChooser;
import fr.tp.inf112.projects.robotsim.network.PersistenceClient;
import fr.tp.inf112.projects.robotsim.network.PersistenceRequest;

public class RemoteFileCanvasChooser extends FileCanvasChooser {

    public RemoteFileCanvasChooser(String fileExtension, String documentTypeLabel) {
        super(fileExtension, documentTypeLabel);
    }

    public RemoteFileCanvasChooser(Component viewer, String fileExtension, String documentTypeLabel) {
        super(viewer, fileExtension, documentTypeLabel);
    }

    @Override
    protected String browseCanvases(boolean open) throws IOException {
        if (open) {
            return chooseFileToOpen();
        }
        return chooseSaveFileName();
    }

    private String chooseFileToOpen() throws IOException {
        Object response = PersistenceClient.sendRequest(PersistenceRequest.LIST_FILES);
        if (!(response instanceof String[])) {
            throw new IOException("The server did not return a list of filenames.");
        }
        String[] fileNames = (String[]) response;

        if (fileNames.length == 0) {
            JOptionPane.showMessageDialog(getViewer(),
                "No factories have been saved on the server yet.",
                "Open factory", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }

        // selectionValues makes this dialog display a dropdown.
        return (String) JOptionPane.showInputDialog(
            getViewer(), "Choose a factory saved on the server:", "Open factory",
            JOptionPane.QUESTION_MESSAGE, null, fileNames, fileNames[0]);
    }

    private String chooseSaveFileName() {
        while (true) {
            String name = JOptionPane.showInputDialog(getViewer(), "Enter the factory filename:");
            if (name == null) {
                return null; // The user cancelled.
            }

            name = name.trim();
            if (!name.isEmpty() && !name.equals(".") && !name.equals("..")
                    && !name.contains("/") && !name.contains("\\")) {
                // Inherited newCanvasId() adds the .factory extension if needed.
                return name;
            }

            JOptionPane.showMessageDialog(getViewer(),
                "Enter a filename without folders, for example my-factory.",
                "Invalid filename", JOptionPane.WARNING_MESSAGE);
        }
    }
}
```

**2. Save and load over the network: RemoteFactoryPersistenceManager**

The viewer keeps calling the same `persist()` and `read()` interface methods. This implementation uses the network.

- `persist(canvasModel)` sends a `Factory` and waits for `Boolean.TRUE`, meaning the server finished writing it. A server-side failure becomes an `IOException` instead of appearing to be a successful save.
- `read(canvasId)` sends the filename and returns the received factory. `Factory` implements `Canvas`, so it satisfies the method's return type.
- `delete()` explicitly remains unsupported, as permitted by the assignment.

The constructor passes the chooser to the superclass. The viewer retrieves it through `getCanvasChooser()`. The manager does not display the dialogs itself.

Source: [RemoteFactoryPersistenceManager.java](../src/fr/tp/inf112/projects/robotsim/model/RemoteFactoryPersistenceManager.java)

```java
package fr.tp.inf112.projects.robotsim.model;

import java.io.IOException;

import fr.tp.inf112.projects.canvas.model.Canvas;
import fr.tp.inf112.projects.canvas.model.CanvasChooser;
import fr.tp.inf112.projects.canvas.model.impl.AbstractCanvasPersistenceManager;
import fr.tp.inf112.projects.robotsim.network.PersistenceClient;

public class RemoteFactoryPersistenceManager extends AbstractCanvasPersistenceManager {

    public RemoteFactoryPersistenceManager(CanvasChooser canvasChooser) {
        super(canvasChooser);
    }

    @Override
    public Canvas read(String canvasId) throws IOException {
        Object response = PersistenceClient.sendRequest(canvasId);
        if (!(response instanceof Factory)) {
            throw new IOException("The server did not return a factory model.");
        }
        return (Factory) response;
    }

    @Override
    public void persist(Canvas canvasModel) throws IOException {
        if (!(canvasModel instanceof Factory)) {
            throw new IOException("Only factory models can be saved by this manager.");
        }

        Object response = PersistenceClient.sendRequest(canvasModel);
        if (!Boolean.TRUE.equals(response)) {
            throw new IOException("The server did not confirm that the factory was saved.");
        }
    }

    @Override
    public boolean delete(Canvas canvasModel) throws IOException {
        throw new UnsupportedOperationException("Remote deletion is not used by the simulator.");
    }
}
```

**3. Share the socket code: PersistenceClient**

Both the chooser and remote manager need to exchange objects with the same server. `sendRequest()` contains that common code once.

It connects, constructs and flushes an output stream, constructs an input stream, writes the request, flushes, reads one response, and closes all resources. Each request uses a fresh connection. An output-first stream setup avoids both programs waiting for each other's serialization header.

If the response is an `IOException`, it is thrown locally so the existing viewer can report failure. `ClassNotFoundException` is wrapped in `IOException`. Connection and read timeouts prevent indefinite waiting.

The defaults are localhost and port 5050. For a server on another computer, set the simulator VM argument `-Drobotsim.server.host=SERVER_ADDRESS`. Set `-Drobotsim.server.port=PORT` in both launches to change the port.

Source: [PersistenceClient.java](../src/fr/tp/inf112/projects/robotsim/network/PersistenceClient.java)

```java
package fr.tp.inf112.projects.robotsim.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/** Shared socket exchange used by the remote manager and file chooser. */
public final class PersistenceClient {

    private PersistenceClient() {
    }

    public static Object sendRequest(Object request) throws IOException {
        String host = System.getProperty("robotsim.server.host", "localhost");
        int port = Integer.getInteger("robotsim.server.port", 5050);

        try (Socket server = new Socket()) {
            server.connect(new InetSocketAddress(host, port), 5000);
            server.setSoTimeout(15000);

            // Send the stream header before waiting for the server's header.
            try (ObjectOutputStream out = new ObjectOutputStream(server.getOutputStream())) {
                out.flush();

                try (ObjectInputStream in = new ObjectInputStream(server.getInputStream())) {
                    out.writeObject(request);
                    out.flush();

                    Object response = in.readObject();
                    if (response instanceof IOException) {
                        throw (IOException) response;
                    }
                    return response;
                }
            }
        } catch (ClassNotFoundException ex) {
            throw new IOException("Could not deserialize the server response.", ex);
        }
    }
}
```

**4. Give file listing its own request: PersistenceRequest**

The assignment says every `String` represents a filename to read. Sending the string "LIST_FILES" would therefore be ambiguous. An enum is a separate, serializable type. Both programs recognize the `LIST_FILES` value as a request for the filename array.

Source: [PersistenceRequest.java](../src/fr/tp/inf112/projects/robotsim/network/PersistenceRequest.java)

```java
package fr.tp.inf112.projects.robotsim.network;

/** Extra requests that must not be confused with String filenames. */
public enum PersistenceRequest {
    LIST_FILES
}
```

**5. Process requests and access files: PersistenceServer**

The server creates the original `FactoryPersistenceManager` with a null chooser: it already gets filenames from network requests and never asks a user to choose a file.

`processRequest()` distinguishes three requests:

| Request | Server operation | Response |
| --- | --- | --- |
| `PersistenceRequest.LIST_FILES` | List the current folder's regular `.factory` files | Sorted `String[]` |
| `String` | Call the existing manager's `read(filename)` | Loaded `Factory` |
| `Factory` | Call the existing manager's `persist(factory)` | `Boolean.TRUE` after the write succeeds |

A failure is logged and returned as an `IOException`. After the connection closes, the loop accepts the next client. The server handles one request at a time.

`listFactoryFiles()` only lists the server's working directory, not nested folders. Filename validation keeps requests in that directory. `FactoryPersistenceManager` continues to perform the existing disk serialization; it was not rewritten.

Source: [PersistenceServer.java](../src/fr/tp/inf112/projects/robotsim/network/PersistenceServer.java)

```java
package fr.tp.inf112.projects.robotsim.network;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.tp.inf112.projects.robotsim.model.Factory;
import fr.tp.inf112.projects.robotsim.model.FactoryPersistenceManager;

public class PersistenceServer {

    private static final Logger LOGGER = Logger.getLogger(PersistenceServer.class.getName());

    public static void main(String[] args) throws IOException {
        FactoryPersistenceManager persistenceManager = new FactoryPersistenceManager(null);
        int port = Integer.getInteger("robotsim.server.port", 5050);

        try (ServerSocket server = new ServerSocket(port)) {
            LOGGER.info("Persistence server listening on port " + port
                + "; files are stored in " + Path.of(".").toAbsolutePath().normalize());

            while (true) {
                try (
                    Socket client = server.accept();
                    ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream())
                ) {
                    client.setSoTimeout(15000);
                    out.flush();

                    try (ObjectInputStream in = new ObjectInputStream(client.getInputStream())) {
                        Object response;
                        try {
                            Object request = in.readObject();
                            response = processRequest(request, persistenceManager);
                        } catch (IOException | ClassNotFoundException ex) {
                            LOGGER.log(Level.WARNING, "Failed to process client request", ex);
                            // Return the failure so the simulator can display an error.
                            response = new IOException("Server could not complete the request: "
                                + ex.getMessage());
                        }

                        out.writeObject(response);
                        out.flush();
                    }
                } catch (IOException ex) {
                    LOGGER.log(Level.WARNING, "Client connection failed", ex);
                }
            }
        }
    }

    private static Object processRequest(Object request,
                                         FactoryPersistenceManager persistenceManager)
            throws IOException {
        if (request == PersistenceRequest.LIST_FILES) {
            return listFactoryFiles();
        }

        if (request instanceof String) {
            String id = (String) request;
            validateFileName(id);
            return persistenceManager.read(id);
        }

        if (request instanceof Factory) {
            Factory factory = (Factory) request;
            validateFileName(factory.getId());
            persistenceManager.persist(factory);
            LOGGER.info("Saved factory to " + factory.getId());
            // Confirm success only after the file has been written and closed.
            return Boolean.TRUE;
        }

        throw new IOException("Unsupported persistence request.");
    }

    private static String[] listFactoryFiles() throws IOException {
        // "." means the working folder of this server process.
        File serverFolder = new File(".");
        File[] entries = serverFolder.listFiles();
        if (entries == null) {
            throw new IOException("Could not list the server's working folder.");
        }

        List<String> fileNames = new ArrayList<>();
        for (File entry : entries) {
            if (entry.isFile() && entry.getName().endsWith(".factory")) {
                fileNames.add(entry.getName());
            }
        }

        Collections.sort(fileNames);
        return fileNames.toArray(new String[0]);
    }

    private static void validateFileName(String name) throws IOException {
        if (name == null || name.isBlank() || name.equals(".") || name.equals("..")
                || name.contains("/") || name.contains("\\")) {
            throw new IOException("Use a filename in the server's working directory, without folders.");
        }
    }
}
```

**6. Connect the new objects to the viewer**

`SimulatorApplication` now creates the remote chooser and remote manager:

```java
final RemoteFileCanvasChooser canvasChooser =
    new RemoteFileCanvasChooser("factory", "Puck Factory");
final RemoteFactoryPersistenceManager persistenceManager =
    new RemoteFactoryPersistenceManager(canvasChooser);
final SimulatorController controller = new SimulatorController(factory, persistenceManager);
final Component factoryViewer = new CanvasViewer(controller);
canvasChooser.setViewer(factoryViewer);
```

The viewer's existing calls to `read()` and `persist()` stay the same. The object receiving those calls is now the remote manager.

**Example: saving**

```text
Save Canvas
  → chooser.newCanvasId()
  → our browseCanvases(false)
  → chooseSaveFileName(): user enters "my-factory"
  → inherited newCanvasId() adds ".factory"
  → viewer sets the model's ID
  → remote manager sends the Factory
  → server calls its original file manager
  → file is written; server returns true
```

**Example: opening**

```text
Open Canvas
  → chooser.choseCanvas()
  → our browseCanvases(true)
  → chooseFileToOpen() asks the server for filenames
  → user chooses "my-factory.factory"
  → remote manager sends that filename
  → server reads the file and returns the Factory
  → viewer displays the loaded model
```

**Validation**

All 31 Java source files compiled with Java 17 compatibility. A client and server ran in separate JVMs and different temporary working directories. Checks passed for saving a factory containing robots and both pathfinder implementations, loading it, overwriting it, listing only top-level model files, and reporting missing files, failed writes, unsupported requests, and invalid filenames. The server continued serving after these errors. The tests confirmed that no model file was written in the client's working folder.

The Swing dialogs were not clicked through interactively during these tests. The obsolete practice `Client.java` has been removed. The server is now named `PersistenceServer`, and its launch configuration points to that class.

**How the server sees local files**

`new File(".")` represents the current working folder of the server JVM. It does not create a file. `serverFolder.listFiles()` asks the operating system for the entries in that folder. The ordinary `for` loop keeps entries that are files and whose names end in `.factory`. It sorts the names, then converts the list to a `String[]` for the response.

The supplied PersistenceServer launch sets the working folder to the simulator project directory in Downloads. If the server runs on another computer, these filesystem operations happen on that computer. Only the resulting names are sent across the socket.

**Where the Save/Open calls really happen**

`SimulatorApplication` creates objects and connects them. Creating the manager does not itself save anything. `SimulatorController` stores the manager in its `persistenceManager` field and returns it from `getPersistenceManager()`.

The menu action code is in `CanvasViewer.java`, bundled in `libs/canvas-viewer.jar`. Its Save operation eventually executes this exact call:

```java
controller.getPersistenceManager().persist(controller.getCanvas());
```

`getPersistenceManager()` returns the RemoteFactoryPersistenceManager object supplied at startup. `getCanvas()` returns the current Factory. Java therefore calls the remote implementation of `persist()` with that factory.

On Open, the viewer obtains a filename from the chooser and then executes:

```java
initCanvasModel(persistenceManager.read(chosenCanvasId));
```

The remote `read()` obtains the Factory from the server. `initCanvasModel()` gives the loaded factory to the controller and refreshes the view.

The controller's field is declared as the `CanvasPersistenceManager` interface. The object held in that field is our `RemoteFactoryPersistenceManager`. The interface lets the existing viewer call either the original local implementation or the new remote implementation without rewriting its menu actions.
