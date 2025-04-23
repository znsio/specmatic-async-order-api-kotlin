package com.component.orders.contract

import com.component.orders.dal.OrderRepository
import com.component.orders.models.Order
import com.component.orders.models.OrderStatus
import io.specmatic.kafka.Expectation
import io.specmatic.kafka.KafkaMock
import io.specmatic.test.SpecmaticContractTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy
import java.io.File
import java.time.Duration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrderCreateRequestContractTests : SpecmaticContractTest {
    @Value("\${schema.registry.url}")
    lateinit var schemaRegistryUrl: String

    @Autowired
    private lateinit var orderRepository: OrderRepository

    private val kafkaAndSchemaRegistry = kafkaAndSchemaRegistry()
    private lateinit var kafkaMock: KafkaMock

    @BeforeAll
    fun setUp() {
        kafkaAndSchemaRegistry.start()

        System.setProperty("host", APPLICATION_HOST)
        System.setProperty("port", APPLICATION_PORT)
        System.setProperty("endpointsAPI", ACTUATOR_MAPPINGS_ENDPOINT)
        System.setProperty("CONSUMER_GROUP_ID", CONSUMER_GROUP_IDENTIFIER)
        System.setProperty("SCHEMA_REGISTRY_URL", schemaRegistryUrl)

        // Connect Specmatic Kafka Mock with the kafka broker and set the expectations
        kafkaMock = KafkaMock.connectWithBroker(KAFKA_MOCK_HOST, KAFKA_MOCK_PORT)
        kafkaMock.setExpectations(
            listOf(
                Expectation("create-order-request", EXPECTED_NUMBER_OF_MESSAGES),
                Expectation("create-order-reply", EXPECTED_NUMBER_OF_MESSAGES)
            )
        )
    }

    @AfterAll
    fun tearDown() {
        kafkaAndSchemaRegistry.stop()

        val result = kafkaMock.stop()
        assertThat(result.success).withFailMessage(result.errors.joinToString()).isTrue

        assertThat(orderFromDBWithId(3).status).isEqualTo(OrderStatus.Completed)
    }

    private fun orderFromDBWithId(orderId: Int): Order = orderRepository.findById(orderId)
        .orElseThrow { RuntimeException("Order with id $orderId not found") }

    private fun kafkaAndSchemaRegistry(): DockerComposeContainer<*> {
        return DockerComposeContainer(
            File("src/test/resources/docker-compose.yaml")
        ).withLocalCompose(true).waitingFor(
            "schema-registry",
            LogMessageWaitStrategy()
                .withRegEx(".*(?i)server started, listening for requests.*")
                .withStartupTimeout(Duration.ofSeconds(60))
        )
    }

    companion object {
        private const val APPLICATION_HOST = "localhost"
        private const val APPLICATION_PORT = "8080"
        private const val KAFKA_MOCK_HOST = "localhost"
        private const val KAFKA_MOCK_PORT = 9092

        private const val ACTUATOR_MAPPINGS_ENDPOINT = "http://$APPLICATION_HOST:$APPLICATION_PORT/actuator/mappings"

        private const val EXPECTED_NUMBER_OF_MESSAGES = 1
        private const val CONSUMER_GROUP_IDENTIFIER = "order-consumer-1234"
    }
}
