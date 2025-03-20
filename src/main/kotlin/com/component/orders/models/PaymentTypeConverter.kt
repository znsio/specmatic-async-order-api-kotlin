package com.component.orders.models

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class PaymentTypeConverter : AttributeConverter<PaymentType, String> {
    override fun convertToDatabaseColumn(paymentType: PaymentType?): String = paymentType.toString()

    override fun convertToEntityAttribute(paymentTypeInDb: String?): PaymentType {
        return (paymentTypeInDb?.let { PaymentType.valueOf(it) } ?: PaymentType.COD)
    }
}
