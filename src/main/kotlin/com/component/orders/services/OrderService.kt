package com.component.orders.services

import com.component.orders.dal.OrderRepository
import com.component.orders.models.*
import com.component.orders.models.OrderStatus.*
import com.fasterxml.jackson.databind.ObjectMapper
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import jakarta.annotation.PostConstruct
import order.CreateOrderRequest
import order.PaymentType
import order.Product
import order.StatusEnum
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class OrderService(private val jacksonObjectMapper: ObjectMapper) {
    @Value("\${kafka.createOrderRequest.topic}")
    lateinit var kafkaCreateOrderRequestTopic: String

    @Autowired
    private lateinit var repo: OrderRepository

    @Autowired
    private lateinit var kafkaService: KafkaService

    fun get(id: Int): Order {
        return repo.findById(id).orElseThrow {
            RuntimeException("Order with id $id not found")
        }
    }

    fun create(newOrder: NewOrder): Id {
        val order = Order(
            paymentType = newOrder.paymentType,
            products = newOrder.products,
            status = Accepted,
            instructions = newOrder.instructions
        )
        val createdOrder = repo.save(order)

        val producer = kafkaService.producer()

        try {
            val createOrderRequest = CreateOrderRequest.newBuilder()
                .setId(order.id)
                .setPaymentType(PaymentType.valueOf(order.paymentType.toString()))
                .setProducts(
                    order.products.map { Product(it.id, it.quantity) }
                )
                .setStatus(StatusEnum.Accepted)
                .setInstructions(order.instructions)
                .build()

            producer.send(
                ProducerRecord(kafkaCreateOrderRequestTopic, createOrderRequest)
            )
            producer.close()
            return Id(createdOrder.id)
        } catch(e: Throwable) {
            e.printStackTrace()
            throw e
        }
    }
}