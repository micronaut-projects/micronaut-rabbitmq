plugins {
    groovy
    id("io.micronaut.build.internal.rabbitmq-base")
    id("io.micronaut.minimal.application")
}

dependencies {
    testImplementation(projects.micronautRabbitmq)
    testImplementation(libs.testcontainers.spock)
    testImplementation(mn.micronaut.management)
    testImplementation(mn.reactor)

}
