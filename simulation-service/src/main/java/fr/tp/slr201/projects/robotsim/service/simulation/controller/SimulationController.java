package fr.tp.slr201.projects.robotsim.service.simulation.controller;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.RestController;

import fr.tp.inf112.projects.robotsim.model.Factory;
import fr.tp.inf112.projects.robotsim.model.RemoteFactoryPersistenceManager;
import fr.tp.slr201.projects.robotsim.service.simulation.notifier.KafkaFactoryModelChangeNotifier;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
public class SimulationController {
    private static final Logger LOGGER =
        Logger.getLogger(SimulationController.class.getName());

    private final Map<String, Factory> runningFactories = new ConcurrentHashMap<>();

    private final RemoteFactoryPersistenceManager persistenceManager = new RemoteFactoryPersistenceManager(null);

    @Autowired
    private KafkaTemplate<String, Factory> simulationEventTemplate;

    @Autowired
    private KafkaAdmin kafkaAdmin;

    @GetMapping("/simulation/start")
    public boolean startSimulation(@RequestParam("id") String id) {
        if (runningFactories.containsKey(id)) {
            return false;
        }

        Factory factory = null;
        try {
            factory = (Factory) persistenceManager.read(id);
            factory.setNotifier(new KafkaFactoryModelChangeNotifier(
                    factory, simulationEventTemplate, kafkaAdmin));

            runningFactories.put(id, factory);
            factory.startSimulation();
            LOGGER.info("Started simulation for " + id);
            return true;
        } catch (IOException | RuntimeException exception) {
            if (factory != null) {
                runningFactories.remove(id, factory);
                try {
                    factory.stopSimulation();
                } catch (RuntimeException cleanupException) {
                    exception.addSuppressed(cleanupException);
                }
            }
            LOGGER.log(Level.WARNING, "Could not start simulation for " + id, exception);
            return false;
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
