package fr.tp.inf112.projects.robotsim.app;

import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.Collections;

import org.apache.kafka.clients.consumer.KafkaConsumer;

import fr.tp.inf112.projects.robotsim.model.Factory;

import java.time.Duration;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

public class FactorySimulationEventConsumer {

    private final RemoteSimulatorController controller;

    private final KafkaConsumer<String, String> consumer;

    public FactorySimulationEventConsumer(
            RemoteSimulatorController controller) {
        this.controller = controller;

        Properties props = new Properties();

        props.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                "localhost:9092");

        props.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                "Factory-Simulation-Group");

        props.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest");

        props.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);

        props.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);


        this.consumer = new KafkaConsumer<>(props);

        Factory factory = (Factory) controller.getCanvas();

        String topicName = "simulation-" + factory.getId();

        this.consumer.subscribe(Collections.singletonList(topicName));


    }

    public void consumeMessages() {
        try {
            while (controller.isAnimationRunning()) {
                ConsumerRecords<String, String> records =
                        consumer.poll(Duration.ofMillis(100));

                for (ConsumerRecord<String, String> record : records) {
                    controller.setCanvas(record.value());
                }
            }
        } finally {
            consumer.close();
        }
    }
}
