package fr.tp.slr201.projects.robotsim.service.simulation.controller;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.io.IOException;

import org.springframework.web.bind.annotation.RestController;

import fr.tp.inf112.projects.robotsim.model.Factory;
import fr.tp.inf112.projects.robotsim.model.RemoteFactoryPersistenceManager;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
public class SimulationController {
    private static final Logger LOGGER =
        Logger.getLogger(SimulationController.class.getName());

    private final Map<String, Factory> runningFactories = new ConcurrentHashMap<>();

    private final RemoteFactoryPersistenceManager persistenceManager = new RemoteFactoryPersistenceManager(null);

    @GetMapping("/simulation/start")
    public boolean startSimulation(@RequestParam("id") String id) {
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

    @GetMapping("/simulation")
    public Factory getFactory(@RequestParam("id") String id) {
        return runningFactories.get(id);
    }


    @GetMapping("/simulation/stop")
    public boolean stopSimulation(@RequestParam("id") String id){
        Factory factory = runningFactories.get(id);

        if(factory == null) {
            LOGGER.warning(" No running factory found for" + id);
            return false;
        }

        factory.stopSimulation();
        runningFactories.remove(id);

        LOGGER.info("Stopped simulation for " + id);
        return true;
    }

}
