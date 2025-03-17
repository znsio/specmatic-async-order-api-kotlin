package com.component.orders.models

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class ProductsConverter : AttributeConverter<List<Product>, String> {
    private val objectMapper = jacksonObjectMapper()

    override fun convertToDatabaseColumn(attribute: List<Product>?): String {
        return objectMapper.writeValueAsString(attribute)
    }

    override fun convertToEntityAttribute(dbData: String?): List<Product> {
        return dbData?.let {
            try {
                objectMapper.readValue<List<Product>>(it)
            } catch (_: MismatchedInputException) {
                val jsonWithoutDoubleEncoding = objectMapper.readValue<String>(it)
                objectMapper.readValue<List<Product>>(jsonWithoutDoubleEncoding)
            }
        } ?: emptyList()
    }
}