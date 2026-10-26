package fr.tp.inf112.projects.robotsim.app;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import fr.tp.inf112.projects.canvas.controller.Observer;
import fr.tp.inf112.projects.canvas.model.Canvas;
import fr.tp.inf112.projects.canvas.model.CanvasPersistenceManager;
import fr.tp.inf112.projects.robotsim.model.Factory;

import java.io.IOException;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import fr.tp.inf112.projects.robotsim.model.FactoryJsonMapper;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;





public class RemoteSimulatorController extends SimulatorController{

	public RemoteSimulatorController(Factory factoryModel, CanvasPersistenceManager persistenceManager) {
		super(factoryModel, persistenceManager);
	}
	
	private static final Logger LOGGER = Logger.getLogger(RemoteSimulatorController.class.getName());
	
	private final HttpClient httpClient = HttpClient.newHttpClient();
	
	private final ObjectMapper objectMapper = FactoryJsonMapper.createObjectMapper();

	private volatile Thread pollingThread;
	
	@Override
	public void setCanvas(Canvas canvasModel) {
		Factory previousFactory = (Factory) getCanvas();

		super.setCanvas(canvasModel);

		Factory newFactory = (Factory) getCanvas();

		if (previousFactory != null && newFactory != null
				&& previousFactory != newFactory) {
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
	public void startAnimation(){ 
		Factory factoryModel = (Factory) getCanvas();
		if (factoryModel == null || factoryModel.getId() == null || factoryModel.getId().isBlank()) {
			LOGGER.warning("Save or open a factory before starting the remote simulation.");
			return;
		}
		String id = factoryModel.getId();
		
		String encodedId = URLEncoder.encode(id, StandardCharsets.UTF_8);
		
		String address = "http://localhost:8080/simulation/start?id=" + encodedId;
		
		URI  uri = URI.create(address);
		
		HttpRequest  request = HttpRequest.newBuilder(uri)
				.GET()
				.build();
		try {
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			

			if (response.statusCode() == 200
					&& Boolean.parseBoolean(response.body())) {

				LOGGER.info("Remote simulation started successfully");
				startConsumingEvents();

			} else {
			    LOGGER.warning(
			    		"Could not start remote simulation. Status: "
			            + response.statusCode()
			            + ", body: "
			            + response.body()
			        );
			}
		}
		
		catch (IOException ex) {
			LOGGER.warning("Could not start simulation: " + ex.getMessage());
		}
		
		catch(InterruptedException ex) {
		    
			Thread.currentThread().interrupt();

		    LOGGER.warning(ex.getMessage());
		}
	}
	
	@Override
	public void stopAnimation(){ 
		Factory factoryModel = (Factory) getCanvas();
		if (factoryModel == null || factoryModel.getId() == null || factoryModel.getId().isBlank()) {
			LOGGER.warning("No factory is available to stop remotely.");
			return;
		}
		String id = factoryModel.getId();
		
		String encodedId = URLEncoder.encode(id, StandardCharsets.UTF_8);
		
		String address = "http://localhost:8080/simulation/stop?id=" + encodedId;
		
		URI  uri = URI.create(address);
		
		HttpRequest  request = HttpRequest.newBuilder(uri)
				.GET()
				.build();
		try {
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			

			if (response.statusCode() == 200
					&& Boolean.parseBoolean(response.body())) {

				LOGGER.info("Remote simulation stopped successfully");
				pollingThread = null;

			} else {
			    LOGGER.warning(
			    		"Could not stop remote simulation. Status: "
			            + response.statusCode()
			            + ", body: "
			            + response.body()
			        );
			}
		}
		catch (IOException ex) {
			
		LOGGER.warning("Could not stop simulation: " + ex.getMessage());
		}
		
		catch(InterruptedException ex) {
	    
			Thread.currentThread().interrupt();

			LOGGER.warning(ex.getMessage());
		}
		
	}
	
	private String getFactoryJson(String id) 
			throws IOException, InterruptedException {
		
		
		String encodedId = URLEncoder.encode(id, StandardCharsets.UTF_8);
		
		String address = "http://localhost:8080/simulation?id=" + encodedId;
		
		URI  uri = URI.create(address);
		
		HttpRequest  request = HttpRequest.newBuilder(uri)
				.GET()
				.build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			

		if (response.statusCode() != 200) {
			throw new IOException(
					"Failed to retrieve factory. HTTP status: "
			        + response.statusCode()
			    );
		}
			
		if (response.body() == null || response.body().isBlank()) {
			throw new IOException("Factory response body is empty");
		}
		if ("null".equals(response.body().trim())) {
			throw new IOException("No running factory found for " + id);
		}
			
		return response.body();
		
		
	}
	
	
	private Factory getRemoteFactory(String id) throws IOException, InterruptedException {
		String json = getFactoryJson(id);
		try {
			return objectMapper.readValue(json, Factory.class);
		} catch (JacksonException ex) {
			throw new IOException("Could not parse factory snapshot for " + id, ex);
		}
		
	}

	@Override
	public boolean isAnimationRunning() {
		Thread thread = pollingThread;
		return thread != null && thread.isAlive();
	}

	private Factory refreshOnce(String id) throws IOException, InterruptedException {
		Factory snapshot = getRemoteFactory(id);
		SwingUtilities.invokeLater(() -> setCanvas(snapshot));
		return snapshot;
	}

	private void pollSnapshots(String id) throws IOException, InterruptedException {
		while (true) {
			Factory snapshot = refreshOnce(id);
			if (!snapshot.isSimulationStarted()) {
				return;
			}
			Thread.sleep(100);
		}
	}

	private void startConsumingEvents() {
		pollingThread = new Thread(() -> {
			try {
				FactorySimulationEventConsumer eventConsumer =
						new FactorySimulationEventConsumer(this);

				eventConsumer.consumeMessages();
			} catch (RuntimeException ex) {
				LOGGER.warning(
						"Could not read Kafka updates: " + ex.getMessage());
			}
		}, "factory-kafka-consumer");

		pollingThread.start();
	}

	
	
		
		
			
	

}
