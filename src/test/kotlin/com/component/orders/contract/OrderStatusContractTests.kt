package com.component.orders.contract

import com.component.orders.models.Order
import com.component.orders.models.OrderStatus
import io.specmatic.kafka.CONSUMER_GROUP_ID
import io.specmatic.kafka.EXAMPLES_DIR
import io.specmatic.kafka.KafkaMock
import io.specmatic.kafka.SpecmaticKafkaContractTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestContextManager
import org.springframework.transaction.annotation.Transactional
import java.sql.Connection
import javax.sql.DataSource

private const val ORDER_ID = 3

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class OrderStatusContractTests : SpecmaticKafkaContractTest {
    @Autowired
    private lateinit var dataSource: DataSource

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
            insertIntoDb("INSERT INTO orders (id, payment_type, products, status) VALUES ($ORDER_ID, 'COD', '[{\"id\":1,\"quantity\":10}]', 'Accepted')")

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
            assertThat(getOrderStatusFromDb(ORDER_ID) ).isEqualTo(OrderStatus.Completed)
            kafka.stop()
        }

        private fun getOrderStatusFromDb(orderId: Int): String {
            return fetchFromDb("SELECT status FROM orders WHERE id = $orderId")[0]["STATUS"] as String
        }

        private fun testInstance(): OrderStatusContractTests {
            val testInstance = OrderStatusContractTests()
            val contextManager = TestContextManager(OrderStatusContractTests::class.java)
            contextManager.prepareTestInstance(testInstance)
            return testInstance
        }

        fun insertIntoDb(sql: String) {
            var connection: Connection? = null
            try {
                connection = testInstance().dataSource.connection
                val statement = connection.createStatement()
                statement.execute(sql)
            } finally {
                connection?.close()
            }
        }

        fun fetchFromDb(sql: String): List<Map<String, Any>> {
            var connection: Connection? = null
            val results = mutableListOf<Map<String, Any>>()
            try {
                connection = testInstance().dataSource.connection
                val statement = connection.createStatement()
                val resultSet = statement.executeQuery(sql)
                val metaData = resultSet.metaData
                val columnCount = metaData.columnCount

                while (resultSet.next()) {
                    val row = mutableMapOf<String, Any>()
                    for (i in 1..columnCount) {
                        row[metaData.getColumnName(i)] = resultSet.getObject(i)
                    }
                    results.add(row)
                }
            } finally {
                connection?.close()
            }
            return results
        }
    }
}