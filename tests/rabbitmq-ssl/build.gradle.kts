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

    testImplementation(mnTest.micronaut.test.spock)
    testImplementation(mnTestResources.testcontainers.core)
    testImplementation(mnTestResources.testcontainers.rabbitmq)
    testImplementation(libs.awaitility)
}

micronaut {
    version(libs.versions.micronaut.platform.get())
    testRuntime("junit5")
}
