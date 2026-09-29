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

public class HelloServer {

    private static final Logger LOGGER = Logger.getLogger(HelloServer.class.getName());

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
