package com.component.orders.services

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Properties

@Component
class KafkaService {
    @Value("\${kafka.bootstrap-servers}")
    lateinit var kafkaBootstrapServers: String

    fun producer(): KafkaProducer<String, String> {
        return KafkaProducer<String, String>(producerProperties())
    }

    fun consumer(): KafkaConsumer<String, String> {
        return KafkaConsumer<String, String>(consumerProperties())
    }

    private fun producerProperties(): Properties {
        val props = Properties()
        props["bootstrap.servers"] = kafkaBootstrapServers
        props["key.serializer"] = "org.apache.kafka.common.serialization.StringSerializer"
        props["value.serializer"] = "org.apache.kafka.common.serialization.StringSerializer"
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = kafkaBootstrapServers
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
        return props
    }

    private fun consumerProperties(): Properties {
        val props = producerProperties()
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = kafkaBootstrapServers
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
        props[ConsumerConfig.GROUP_ID_CONFIG] = "order-service"
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        props[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "true"

        return props
    }
}