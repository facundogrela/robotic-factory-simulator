package fr.tp.inf112.projects.robotsim.app;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import fr.tp.inf112.projects.canvas.model.Canvas;
import fr.tp.inf112.projects.canvas.model.CanvasPersistenceManager;
import fr.tp.inf112.projects.robotsim.model.Factory;
import fr.tp.inf112.projects.robotsim.model.FactoryJsonMapper;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

public class RemoteSimulatorController extends SimulatorController {

    private static final Logger LOGGER = Logger.getLogger(RemoteSimulatorController.class.getName());

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = FactoryJsonMapper.createObjectMapper();
    private volatile Thread consumerThread;

    public RemoteSimulatorController(Factory factoryModel, CanvasPersistenceManager persistenceManager) {
        super(factoryModel, persistenceManager);
    }

    @Override
    public void setCanvas(Canvas canvasModel) {
        Factory previousFactory = (Factory) getCanvas();
        super.setCanvas(canvasModel);
        Factory newFactory = (Factory) getCanvas();

        if (previousFactory != null && newFactory != null && previousFactory != newFactory) {
            newFactory.setNotifier(previousFactory.getNotifier());
        }
        if (newFactory != null) {
            newFactory.notifyObservers();
        }
    }

    public void setCanvas(String json) {
        try {
            Factory snapshot = objectMapper.readValue(json, Factory.class);
            SwingUtilities.invokeLater(() -> setCanvas(snapshot));
        } catch (JacksonException ex) {
            LOGGER.warning("Could not parse factory update: " + ex.getMessage());
        }
    }

    @Override
    public void startAnimation() {
        if (isAnimationRunning() || !sendCommand("start")) {
            return;
        }

        consumerThread = new Thread(() -> {
            try {
                new FactorySimulationEventConsumer(this).consumeMessages();
            } catch (RuntimeException ex) {
                LOGGER.warning("Could not read Kafka updates: " + ex.getMessage());
            }
        }, "factory-kafka-consumer");
        consumerThread.start();
    }

    @Override
    public void stopAnimation() {
        if (sendCommand("stop")) {
            consumerThread = null;
        }
    }

    @Override
    public boolean isAnimationRunning() {
        Thread thread = consumerThread;
        return thread != null && thread.isAlive();
    }

    private boolean sendCommand(String command) {
        Factory factory = (Factory) getCanvas();
        if (factory == null || factory.getId() == null || factory.getId().isBlank()) {
            LOGGER.warning("Save or open a factory before controlling the simulation.");
            return false;
        }

        String id = URLEncoder.encode(factory.getId(), StandardCharsets.UTF_8);
        URI uri = URI.create("http://localhost:8080/simulation/" + command + "?id=" + id);
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && Boolean.parseBoolean(response.body())) {
                return true;
            }
            LOGGER.warning("Could not " + command + " simulation. Status: "
                    + response.statusCode() + ", body: " + response.body());
        } catch (IOException ex) {
            LOGGER.warning("Could not " + command + " simulation: " + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.warning("Simulation request interrupted.");
        }
        return false;
    }
}
