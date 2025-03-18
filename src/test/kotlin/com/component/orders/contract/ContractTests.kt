package com.component.orders.contract

import com.component.orders.dal.OrderRepository
import com.component.orders.models.Order
import com.component.orders.models.OrderStatus
import io.specmatic.kafka.CONSUMER_GROUP_ID
import io.specmatic.kafka.Expectation
import io.specmatic.kafka.KafkaMock
import io.specmatic.test.SpecmaticContractTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestContextManager

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class ContractTests : SpecmaticContractTest {
    @Autowired
    private lateinit var orderRepository: OrderRepository

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
            val orderId = 3
            assertThat( getOrderFromDb(orderId).status ).isEqualTo(OrderStatus.Accepted)
            val result = kafkaMock!!.stop()
            assertThat(result.success).withFailMessage(result.errors.joinToString()).isTrue
        }

        private fun getOrderFromDb(orderId: Int): Order = testInstance().orderRepository.findById(orderId)
            .orElseThrow { RuntimeException("Order with id $orderId not found") }

        private fun testInstance(): ContractTests {
            val testInstance = ContractTests()
            val contextManager = TestContextManager(ContractTests::class.java)
            contextManager.prepareTestInstance(testInstance)
            return testInstance
        }
    }
}