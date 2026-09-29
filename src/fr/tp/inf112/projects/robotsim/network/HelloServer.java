package fr.tp.inf112.projects.robotsim.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class HelloServer {

    public static void main(String[] args) throws IOException {
        try (ServerSocket server = new ServerSocket(5050)) {
            System.out.println("Waiting for a client on port 5050...");

            // accept() waits until a client connects.
            try (
                Socket client = server.accept();
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8)
                );
                PrintWriter out = new PrintWriter(
                    client.getOutputStream(), true, StandardCharsets.UTF_8
                )
            ) {
                System.out.println("Client connected");

                // Each message ends with a newline, written by println().
                String message = in.readLine();

                if (message != null) {
                    String response = "I received " + message + "! ";
                    out.println(response);
                }
            }
        }
    }
}
