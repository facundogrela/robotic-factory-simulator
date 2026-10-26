# Kafka client libraries

These runtime JARs let the plain Java simulator consume Kafka events in Eclipse
without depending on a machine-specific installation path.

| Maven artifact | Version |
| --- | --- |
| `org.apache.kafka:kafka-clients` | `3.8.1` |
| `org.lz4:lz4-java` | `1.8.0` |
| `org.xerial.snappy:snappy-java` | `1.1.10.5` |
| `com.github.luben:zstd-jni` | `1.5.6-4` |
| `org.slf4j:slf4j-api` | `2.0.18` |

The Kafka client and compression JARs come from the Kafka `3.8.1` distribution.
The SLF4J API version matches the Spring service's Maven dependencies: the service
also references the simulator project, so the older SLF4J `1.7` API from Kafka's
distribution would conflict with Spring's Logback provider.

Keep these JARs and `simulator/.classpath` together when importing or updating the
project. Licenses and notices are included in the upstream JARs.
