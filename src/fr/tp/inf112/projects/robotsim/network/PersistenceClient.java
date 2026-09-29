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
