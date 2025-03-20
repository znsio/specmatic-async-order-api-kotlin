package com.component.orders.models

import jakarta.persistence.*
import jakarta.persistence.Id

@Entity
@Table(name = "orders")
data class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "entity_seq_generator")
    @SequenceGenerator(name = "entity_seq_generator", sequenceName = "hibernate_sequence", allocationSize = 1)
    val id: Int = 0,
    val paymentType: PaymentType = PaymentType.COD,
    @Convert(converter = ProductsConverter::class)
    val products: List<Product> = emptyList(),
    var status: OrderStatus = OrderStatus.Accepted,
    val instructions: String? = ""
)