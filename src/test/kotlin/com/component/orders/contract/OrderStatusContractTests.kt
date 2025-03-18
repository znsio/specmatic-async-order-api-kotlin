package com.component.orders.contract

import com.component.orders.dal.OrderRepository
import com.component.orders.models.Order
import com.component.orders.models.OrderStatus
import com.component.orders.models.OrderStatus.Accepted
import com.component.orders.models.PaymentType.COD
import com.component.orders.models.Product
import io.specmatic.kafka.CONSUMER_GROUP_ID
import io.specmatic.kafka.EXAMPLES_DIR
import io.specmatic.kafka.KafkaMock
import io.specmatic.test.SpecmaticContractTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestContextManager
import org.springframework.transaction.annotation.Transactional

private const val ORDER_ID = 3

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class OrderStatusContractTests : SpecmaticContractTest {
    @Autowired
    private lateinit var orderRepository: OrderRepository

    companion object {
        private const val IN_MEMORY_BROKER_HOST = "localhost"
        private const val IN_MEMORY_BROKER_PORT = 9092
        private const val EXAMPLES_DIRECTORY = "spec/examples"

        private const val APPLICATION_HOST = "localhost"
        private const val APPLICATION_PORT = "8080"
        private const val ACTUATOR_MAPPINGS_ENDPOINT = "http://$APPLICATION_HOST:$APPLICATION_PORT/actuator/mappings"
        private const val CONSUMER_GROUP_IDENTIFIER = "order-consumer-1234"

        private lateinit var kafka: KafkaMock

        @JvmStatic
        @BeforeAll
        @Transactional
        fun setUp() {
            testInstance().orderRepository.save(Order(ORDER_ID, COD, listOf(Product(1, 10)), Accepted))
            System.setProperty(EXAMPLES_DIR, EXAMPLES_DIRECTORY)

            System.setProperty("host", APPLICATION_HOST)
            System.setProperty("port", APPLICATION_PORT)
            System.setProperty("endpointsAPI", ACTUATOR_MAPPINGS_ENDPOINT)
            System.setProperty("filter", "PATH!='/health'")
            System.setProperty(CONSUMER_GROUP_ID, CONSUMER_GROUP_IDENTIFIER)

            // Start Specmatic Kafka Mock and set the expectations
            kafka = KafkaMock.startInMemoryBroker(IN_MEMORY_BROKER_HOST, IN_MEMORY_BROKER_PORT)
        }

        @JvmStatic
        @AfterAll
        fun tearDown() {
            assertThat(getOrderFromDb(ORDER_ID).status ).isEqualTo(OrderStatus.Completed)
            kafka.stop()
        }

        private fun getOrderFromDb(orderId: Int): Order = testInstance().orderRepository.findById(orderId)
            .orElseThrow { RuntimeException("Order with id $orderId not found") }

        private fun testInstance(): OrderStatusContractTests {
            val testInstance = OrderStatusContractTests()
            val contextManager = TestContextManager(OrderStatusContractTests::class.java)
            contextManager.prepareTestInstance(testInstance)
            return testInstance
        }
    }
}