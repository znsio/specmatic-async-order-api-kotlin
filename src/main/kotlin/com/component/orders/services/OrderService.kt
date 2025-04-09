package com.component.orders.services

import com.component.orders.dal.OrderRepository
import com.component.orders.models.*
import com.component.orders.models.OrderStatus.*
import com.fasterxml.jackson.databind.ObjectMapper
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
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

    @Value("\${schema.registry.url}")
    lateinit var schemaRegistryUrl: String

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
            val schema: Schema = fetchAvroSchemaForCreateOrderRequest()
            val orderRecord: GenericRecord = orderToGenericRecord(order, schema)

            producer.send(
                ProducerRecord(kafkaCreateOrderRequestTopic, orderRecord)
            )
            producer.close()
            return Id(createdOrder.id)
        } catch(e: Throwable) {
            println("error while fetching schema --> ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    private fun fetchAvroSchemaForCreateOrderRequest(): Schema {
        val client: SchemaRegistryClient = CachedSchemaRegistryClient(schemaRegistryUrl, 100)
        val latestSchemaMetadata = client.getLatestSchemaMetadata("CreateOrderRequest-value")
        val schema: Schema = Schema.Parser().parse(latestSchemaMetadata.schema)
        return schema
    }

    private fun orderToGenericRecord(order: Order, schema: Schema): GenericRecord {
        val paymentTypeSchema = schema.getField("paymentType").schema()
        val statusSchema = schema.getField("status").schema()
        val productsSchema = schema.getField("products").schema()
        val productItemSchema = productsSchema.elementType

        val avroProducts = order.products.map { product ->
            GenericData.Record(productItemSchema).apply {
                put("id", product.id)
                put("quantity", product.quantity)
            }
        }

        return GenericData.Record(schema).apply {
            put("id", order.id)
            put("instructions", order.instructions)
            put("paymentType", GenericData.EnumSymbol(paymentTypeSchema, order.paymentType.name))
            put("status", GenericData.EnumSymbol(statusSchema, order.status.name))
            put("products", avroProducts)
        }
    }
}