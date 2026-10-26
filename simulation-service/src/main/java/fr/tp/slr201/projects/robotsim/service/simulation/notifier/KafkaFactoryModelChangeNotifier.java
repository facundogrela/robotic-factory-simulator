package fr.tp.slr201.projects.robotsim.service.simulation.notifier;

import fr.tp.inf112.projects.canvas.controller.Observer;
import fr.tp.inf112.projects.robotsim.model.FactoryModelChangedNotifier;

import fr.tp.inf112.projects.robotsim.model.Factory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.config.TopicBuilder;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

public class KafkaFactoryModelChangeNotifier implements FactoryModelChangedNotifier {

	private final Factory factoryModel;
	private final KafkaTemplate<String, Factory> simulationEventTemplate;
	private final String topicName;

	private static final Logger LOGGER =
        Logger.getLogger(KafkaFactoryModelChangeNotifier.class.getName());

	public KafkaFactoryModelChangeNotifier(
			Factory factoryModel,
			KafkaTemplate<String, Factory> simulationEventTemplate,
			KafkaAdmin kafkaAdmin) {

		this.factoryModel = factoryModel;
		this.simulationEventTemplate = simulationEventTemplate;
		this.topicName = "simulation-" + factoryModel.getId();

		kafkaAdmin.createOrModifyTopics(
			TopicBuilder.name(topicName)
				.partitions(1)
				.replicas(1)
				.build()
		);
	}

	@Override
	public void notifyObservers() {
		Message<Factory> message = MessageBuilder
				.withPayload(factoryModel)
				.setHeader(KafkaHeaders.TOPIC, topicName)
				.build();

		simulationEventTemplate.send(message)
				.whenComplete((result, exception) -> {
					if (exception != null) {
						LOGGER.log(
								Level.WARNING,
								"Could not publish factory update to " + topicName,
								exception);
					}
				});
	}

	@Override
	public boolean addObserver(Observer observer) {

		return false;
	}

	@Override
	public boolean removeObserver(Observer observer) {

		return false;
	}

}
