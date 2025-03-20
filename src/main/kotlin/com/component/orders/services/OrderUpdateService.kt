package com.component.orders.services

import com.component.orders.dal.OrderRepository
import com.component.orders.models.OrderStatus
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.*
import java.util.concurrent.Executors

@Component
class OrderUpdateService() {
    val kafkaBootstrapServers: String = "localhost:9092"

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    init {
        val executor = Executors.newSingleThreadExecutor()

        executor.execute {
            listen()
        }

        println("Kafka Consumer started in the background. Exiting main thread...")
    }

    private fun getKafkaConsumer(): KafkaConsumer<String, String> {
        val props = Properties()
        println("Kafka Bootstrap Servers: $kafkaBootstrapServers")

        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = kafkaBootstrapServers
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
        props[ConsumerConfig.GROUP_ID_CONFIG] = "order-service"
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest" // Start consuming from the beginning if no offset is stored
        props[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "true" // Auto-commit offsets

        return KafkaConsumer<String, String>(props)
    }

    fun listen() {
        val consumer = getKafkaConsumer()
        consumer.subscribe(listOf("create-order-reply")) // Change this to your topic name

        println("Listening for messages on topic: your-topic-name")

        while (true) {
            val records = consumer.poll(Duration.ofMillis(1000)) // Poll every second
            for (record in records) {
                processMessage(record.value())
            }
        }
    }

    private fun processMessage(message: String) {
        println("Processing message: $message")
        val orderUpdate = objectMapper.readValue(message, OrderUpdate::class.java)
        val order = orderRepository.findById(orderUpdate.id).orElseThrow {
            RuntimeException("Order with id ${orderUpdate.id} not found")
        }
        order.status = OrderStatus.valueOf(orderUpdate.status)
        orderRepository.save(order)
    }

    data class OrderUpdate(
        val id: Int,
        val status: String
    )
}