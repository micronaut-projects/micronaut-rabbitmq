package io.micronaut.rabbitmq.docs.rpc

import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.micronaut.test.support.TestPropertyProvider
import jakarta.inject.Inject
import reactor.core.publisher.Mono
import io.micronaut.rabbitmq.testcontainers.RabbitMQ

@MicronautTest
@Property(name = "spec.name", value = "RpcUppercaseSpec")
class RpcUppercaseSpec : TestPropertyProvider, AnnotationSpec() {

    @Inject
    lateinit var productClient: ProductClient

    @Test
    fun testRpcUppercase() {
        productClient.send("hello") shouldBe "HELLO"
        Mono.from(productClient.sendReactive("world")).block() shouldBe "WORLD"
    }

    override fun getProperties(): Map<String, String> {
        return RabbitMQ.getProperties()
    }
}
