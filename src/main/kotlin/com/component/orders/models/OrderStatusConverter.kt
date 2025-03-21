package com.component.orders.models

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class OrderStatusConverter : AttributeConverter<OrderStatus, String> {
    override fun convertToDatabaseColumn(orderStatus: OrderStatus?): String = orderStatus.toString()

    override fun convertToEntityAttribute(orderStatusInDb: String?): OrderStatus {
        return (orderStatusInDb?.let { OrderStatus.valueOf(it) } ?: OrderStatus.Accepted)
    }
}
