package com.component.orders.services

import com.component.orders.models.*
import com.component.orders.models.OrderStatus.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

@Service
class OrderService(private val jacksonObjectMapper: ObjectMapper) {
    @Value("\${kafka.bootstrap-servers}")
    lateinit var kafkaBootstrapServers: String

    @Value("\${kafka.createOrderRequest.topic}")
    lateinit var kafkaCreateOrderRequestTopic: String

    private val orders = mutableMapOf<Int, Order>().apply {
        put(1, Order(1, PaymentType.COD, listOf(Product(1, 10)), Completed, "Home address"))
        put(2, Order(2, PaymentType.CreditCard, listOf(Product(2, 20)), InProgress))
    }

    fun get(id: Int): Order {
        println("Inside Get")
        println(orders)
        return orders[id] ?: throw RuntimeException("Order with id $id not found")
    }

    fun create(newOrder: NewOrder): Id {
        println("Inside Create")
        println(orders)
        val id = AtomicInteger(orders.size).incrementAndGet()
        val order = Order(id, newOrder.paymentType, newOrder.products, Accepted, newOrder.instructions)
        orders[id] = order

        val producer = getKafkaProducer()
        producer.send(
            ProducerRecord(
                kafkaCreateOrderRequestTopic,
                jacksonObjectMapper.writeValueAsString(order)
            )
        )
        producer.close()
        println("After Create")
        println(orders)
        return Id(id)
    }

    private fun getKafkaProducer(): KafkaProducer<String, String> {
        val props = Properties()
        println("kafkaBootstrapServers: $kafkaBootstrapServers")
        props["bootstrap.servers"] = kafkaBootstrapServers
        props["key.serializer"] = "org.apache.kafka.common.serialization.StringSerializer"
        props["value.serializer"] = "org.apache.kafka.common.serialization.StringSerializer"
        return KafkaProducer<String, String>(props)
    }
}