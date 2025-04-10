package com.component.orders.services

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroSerializer
import order.CreateOrderReply
import order.CreateOrderRequest
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Properties

@Component
class KafkaService {
    @Value("\${kafka.bootstrap-servers}")
    lateinit var kafkaBootstrapServers: String

    @Value("\${schema.registry.url}")
    lateinit var schemaRegistryUrl: String

    fun producer(): KafkaProducer<String, CreateOrderRequest> = KafkaProducer(producerProperties())

    fun consumer(): KafkaConsumer<String, CreateOrderReply> = KafkaConsumer(consumerProperties())

    private fun producerProperties(): Properties = Properties().apply {
        put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers)
        put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
        put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer::class.java.name)
        put("schema.registry.url", schemaRegistryUrl)
        put("value.subject.name.strategy", "io.confluent.kafka.serializers.subject.TopicRecordNameStrategy")
    }

    private fun consumerProperties(): Properties = producerProperties().apply {
        put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
        put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer::class.java.name)
        put(ConsumerConfig.GROUP_ID_CONFIG, "order-service")
        put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
        put("schema.registry.url", schemaRegistryUrl)
        put("specific.avro.reader", "true")
        put("value.subject.name.strategy", "io.confluent.kafka.serializers.subject.TopicRecordNameStrategy")
    }
}