import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    id("io.micronaut.build.internal.rabbitmq-module")
}

dependencies {
    compileOnly(libs.jsr305)

    api(mn.micronaut.messaging)
    api(mn.micronaut.inject)
    api(libs.managed.amqp.client)

    implementation(mnSerde.micronaut.serde.support)
    implementation(mnSerde.micronaut.serde.jackson)
    implementation(mn.micronaut.retry)
    implementation(mn.reactor)
    implementation(libs.caffeine)

    compileOnly(mn.micronaut.management)
    compileOnly(mnMicrometer.micronaut.micrometer.core)

    testImplementation(libs.testcontainers.spock)
    testImplementation(mn.micronaut.inject.groovy)
    testImplementation(mn.micronaut.inject.java)
    testImplementation(mn.micronaut.management)
    testImplementation(mnTest.micronaut.test.spock)
    testImplementation(mnMicrometer.micronaut.micrometer.core) {
      exclude("io.micronaut.reactor", "micronaut-reactor")
    }
    testRuntimeOnly(mnLogging.logback.classic)
}

tasks {
    named<Test>("test") {
        testLogging.showStandardStreams = true
        testLogging.exceptionFormat = TestExceptionFormat.FULL
    }
}
