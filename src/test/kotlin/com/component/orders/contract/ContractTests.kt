package com.component.orders.contract

import io.specmatic.kafka.CONSUMER_GROUP_ID
import io.specmatic.kafka.Expectation
import io.specmatic.kafka.KafkaMock
import io.specmatic.test.SpecmaticContractTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class ContractTests : SpecmaticContractTest {

    companion object {
        private var kafkaMock: KafkaMock? = null
        private const val APPLICATION_HOST = "localhost"
        private const val APPLICATION_PORT = "8080"
        private const val KAFKA_MOCK_HOST = "localhost"
        private const val KAFKA_MOCK_PORT = 9092
        private const val ACTUATOR_MAPPINGS_ENDPOINT = "http://$APPLICATION_HOST:$APPLICATION_PORT/actuator/mappings"
        private const val EXPECTED_NUMBER_OF_MESSAGES = 1
        private const val CONSUMER_GROUP_IDENTIFIER = "order-consumer-1234"

        @JvmStatic
        @BeforeAll
        fun setUp() {
            System.setProperty("host", APPLICATION_HOST)
            System.setProperty("port", APPLICATION_PORT)
            System.setProperty("endpointsAPI", ACTUATOR_MAPPINGS_ENDPOINT)
            System.setProperty("filter", "PATH!='/health'")
            System.setProperty(CONSUMER_GROUP_ID, CONSUMER_GROUP_IDENTIFIER)

            // Start Specmatic Kafka Mock and set the expectations
            kafkaMock = KafkaMock.startInMemoryBroker(KAFKA_MOCK_HOST, KAFKA_MOCK_PORT)
            kafkaMock?.setExpectations(listOf(Expectation("create-order-request", EXPECTED_NUMBER_OF_MESSAGES)))
        }

        @JvmStatic
        @AfterAll
        fun tearDown() {
            val result = kafkaMock!!.stop()
            assertThat(result.success).withFailMessage(result.errors.joinToString()).isTrue
        }
    }
}