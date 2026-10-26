# Jackson libraries

These runtime JARs are included so the plain Java simulator imports directly into
Eclipse without references to another computer's Maven repository:

| Maven artifact | Version |
| --- | --- |
| `tools.jackson.core:jackson-databind` | `3.1.5` |
| `tools.jackson.core:jackson-core` | `3.1.5` |
| `com.fasterxml.jackson.core:jackson-annotations` | `2.21` |

The versions match the Spring Boot `4.1.1` dependencies in `simulation-service`.
Jackson 3 still uses the `com.fasterxml.jackson.annotation` annotations.
If the service's Jackson versions change, update these JARs and `simulator/.classpath`
together.

The artifacts come from [Maven Central](https://repo.maven.apache.org/maven2/).
Each JAR contains its upstream license and notice in `META-INF/LICENSE` and
`META-INF/NOTICE`.
