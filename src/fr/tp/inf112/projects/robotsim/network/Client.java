package fr.tp.inf112.projects.robotsim.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Client {

    public static void main(String[] args) throws IOException {
        try (
            Socket server = new Socket("localhost", 5050);
            BufferedReader in = new BufferedReader(
                new InputStreamReader(server.getInputStream(), StandardCharsets.UTF_8)
            );
            PrintWriter out = new PrintWriter(
                server.getOutputStream(), true, StandardCharsets.UTF_8
            )
        ) {
            String message = "my message";

            // println() adds a newline and flushes because autoFlush is true.
            out.println(message);

            String response = in.readLine();
            if (response == null) {
                throw new IOException("Server closed the connection without a response.");
            }

            System.out.println(response);
        }
    }
}
