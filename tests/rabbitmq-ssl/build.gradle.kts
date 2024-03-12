plugins {
    groovy
    id("io.micronaut.build.internal.rabbitmq-base")
    id("io.micronaut.minimal.application")
}

dependencies {
    testImplementation(projects.micronautRabbitmq)

    testImplementation(mnSerde.micronaut.serde.support)
    testImplementation(mnSerde.micronaut.serde.jackson)
    testImplementation(mn.micronaut.management)
    testImplementation(mn.reactor)

    testImplementation(libs.testcontainers.spock)
    testImplementation(libs.awaitility)
}
