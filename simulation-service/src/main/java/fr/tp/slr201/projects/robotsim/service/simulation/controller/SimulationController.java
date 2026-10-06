package fr.tp.slr201.projects.robotsim.service.simulation.controller;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.io.IOException;

import org.springframework.web.bind.annotation.RestController;

import fr.tp.inf112.projects.robotsim.model.Factory;
import fr.tp.inf112.projects.robotsim.model.RemoteFactoryPersistenceManager;

@RestController
public class SimulationController {
    private static final Logger LOGGER =
        Logger.getLogger(SimulationController.class.getName());

    private final Map<String, Factory> runningFactories = new ConcurrentHashMap<>();

    private final RemoteFactoryPersistenceManager persistenceManager = new RemoteFactoryPersistenceManager(null);

    public boolean startSimulation(String id) {
        if(runningFactories.containsKey(id))
            return false;
        else {
            try {
                Factory factory = (Factory) persistenceManager.read(id);
                runningFactories.put(id, factory);
                factory.startSimulation();
                LOGGER.info("Started simulation for " + id);
                return true;

            }
            catch (IOException exception) {
                LOGGER.warning("Could not load factory " + id + ": " + exception.getMessage());
                return false;
            }

        }
    }


}
