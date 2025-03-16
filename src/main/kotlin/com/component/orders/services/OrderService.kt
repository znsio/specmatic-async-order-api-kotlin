package com.component.orders.services

import com.component.orders.dal.OrderRepository
import com.component.orders.models.*
import com.component.orders.models.OrderStatus.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*

@Service
class OrderService(private val jacksonObjectMapper: ObjectMapper) {
    @Value("\${kafka.bootstrap-servers}")
    lateinit var kafkaBootstrapServers: String

    @Value("\${kafka.createOrderRequest.topic}")
    lateinit var kafkaCreateOrderRequestTopic: String

    @Autowired
    private lateinit var repo: OrderRepository

    fun get(id: Int): Order {
        return repo.findById(id).orElseThrow {
            RuntimeException("Order with id $id not found")
        }
    }

    fun create(newOrder: NewOrder): Id {
        val order = Order(paymentType = newOrder.paymentType, products = newOrder.products, status = Accepted, instructions = newOrder.instructions)
        val createdOrder = repo.save(order)

        val producer = getKafkaProducer()
        producer.send(
            ProducerRecord(
                kafkaCreateOrderRequestTopic,
                jacksonObjectMapper.writeValueAsString(createdOrder)
            )
        )
        producer.close()
        return Id(createdOrder.id)
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