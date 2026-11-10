# Micronaut RabbitMQ

 [![Maven Central](https://img.shields.io/maven-central/v/io.micronaut.rabbitmq/micronaut-rabbitmq.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.micronaut.rabbitmq%22%20AND%20a:%22micronaut-rabbitmq%22)
[![Build Status](https://travis-ci.org/micronaut-projects/micronaut-rabbitmq.svg?branch=master)](https://travis-ci.org/micronaut-projects/micronaut-rabbitmq)
[![Revved up by Develocity](https://img.shields.io/badge/Revved%20up%20by-Develocity-06A0CE?logo=Gradle&labelColor=02303A)](https://ge.micronaut.io/scans)

 Integrates Micronaut and [RabbitMQ](https://www.rabbitmq.com).

 ## Documentation

 See the [latest](https://micronaut-projects.github.io/micronaut-rabbitmq/latest/guide) or [snapshot](https://micronaut-projects.github.io/micronaut-rabbitmq/snapshot/guide/index.html) documentation for more information.

 ## Fork publishing

 This fork publishes its artifacts to the GitHub Packages Maven registry, so services can consume the
 modified library without waiting for an upstream release:

 ```groovy
 repositories {
     maven {
         url = uri("https://maven.pkg.github.com/stcarolas/micronaut-rabbitmq")
         credentials {
             username = findProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
             password = findProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
         }
     }
 }
 dependencies {
     implementation("io.micronaut.rabbitmq:micronaut-rabbitmq:5.1.1-fork.2")
 }
 ```

 See [GITHUB-PACKAGES.md](GITHUB-PACKAGES.md) for how to publish a new version and for troubleshooting.
