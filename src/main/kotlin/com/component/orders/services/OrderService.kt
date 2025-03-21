package com.component.orders.services

import com.component.orders.dal.OrderRepository
import com.component.orders.models.*
import com.component.orders.models.OrderStatus.*
import com.fasterxml.jackson.databind.ObjectMapper
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
        val order = Order(paymentType = newOrder.paymentType, products = newOrder.products, status = Accepted, instructions = newOrder.instructions)
        val createdOrder = repo.save(order)

        val producer = kafkaService.producer()
        producer.send(
            ProducerRecord(
                kafkaCreateOrderRequestTopic,
                jacksonObjectMapper.writeValueAsString(createdOrder)
            )
        )
        producer.close()
        return Id(createdOrder.id)
    }
}