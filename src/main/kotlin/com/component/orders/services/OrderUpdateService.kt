package com.component.orders.services

import com.component.orders.dal.OrderRepository
import com.component.orders.models.OrderStatus
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.Executors

@Service
class OrderUpdateService() {
    @Value("\${kafka.updateOrderReply.topic}")
    lateinit var kafkaUpdateOrderReplyTopic: String

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var kafkaService: KafkaService

    @PostConstruct
    fun initConsumer() {
        val executor = Executors.newSingleThreadExecutor()

        executor.execute {
            listen()
        }

        println("Kafka Consumer started in the background. Exiting main thread...")
    }

    fun listen() {
        val consumer = kafkaService.consumer()
        consumer.subscribe(listOf(kafkaUpdateOrderReplyTopic))

        println("Listening for messages on topic: $kafkaUpdateOrderReplyTopic")

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