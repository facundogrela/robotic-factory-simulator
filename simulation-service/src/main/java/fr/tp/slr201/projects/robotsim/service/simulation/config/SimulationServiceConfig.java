package fr.tp.slr201.projects.robotsim.service.simulation.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import fr.tp.inf112.projects.robotsim.model.Factory;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class SimulationServiceConfig {

    private static final String BOOTSTRAP_SERVERS = "localhost:9092";

    @Bean
    public ProducerFactory<String, Factory> producerFactory(JsonMapper mapper) {
        Map<String, Object> config = new HashMap<>();

        config.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                BOOTSTRAP_SERVERS);

        JacksonJsonSerializer<Factory> serializer =
                new JacksonJsonSerializer<>(mapper);

        return new DefaultKafkaProducerFactory<>(
                config,
                new StringSerializer(),
                serializer);
    }

    @Bean
    public KafkaTemplate<String, Factory> kafkaTemplate(
            ProducerFactory<String, Factory> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> config = new HashMap<>();
        config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        return new KafkaAdmin(config);
    }
}
