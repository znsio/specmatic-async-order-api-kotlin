package com.component.orders.services

import com.component.orders.dal.OrderRepository
import com.component.orders.models.OrderStatus
import com.fasterxml.jackson.databind.ObjectMapper
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import jakarta.annotation.PostConstruct
import order.CreateOrderReply
import org.apache.avro.Schema
import org.apache.avro.generic.GenericDatumReader
import org.apache.avro.generic.GenericDatumWriter
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io.DecoderFactory
import org.apache.avro.io.EncoderFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.time.Duration
import java.util.concurrent.Executors

@Service
class OrderUpdateService {
    @Value("\${kafka.updateOrderReply.topic}")
    lateinit var kafkaUpdateOrderReplyTopic: String

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var kafkaService: KafkaService

    @PostConstruct
    fun initConsumer() {
        Executors.newSingleThreadExecutor().execute { listen() }
        println("[order-service] Kafka Consumer started in the background. Exiting main thread...")
    }

    private fun listen() {
        kafkaService.consumer().apply {
            subscribe(listOf(kafkaUpdateOrderReplyTopic))
            println("[order-service] Listening for messages on topic: $kafkaUpdateOrderReplyTopic")
            while (true) {
                poll(Duration.ofMillis(1000)).forEach {
                    try {
                        println("[order-service] Received a message on topic '$kafkaUpdateOrderReplyTopic': ${it.value()}")
                        processMessage(it.value())
                    } catch(e: Throwable) {
                        println("[order-service] Skipped processing of the received message since it is invalid: ${e.message}")
                    }
                }
            }
        }
    }

    private fun processMessage(message: CreateOrderReply) {
        println("[order-service] Processing message: $message")
        val orderUpdate = OrderUpdate(
            id = message.id,
            status = message.status.name
        )
        val order = orderRepository.findById(orderUpdate.id).orElseThrow {
            RuntimeException("Order with id ${orderUpdate.id} not found")
        }
        order.status = OrderStatus.valueOf(orderUpdate.status)
        orderRepository.save(order)
    }

    data class OrderUpdate(val id: Int, val status: String)
}