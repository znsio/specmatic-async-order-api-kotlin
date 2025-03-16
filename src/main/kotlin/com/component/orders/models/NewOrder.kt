package com.component.orders.models

import jakarta.validation.constraints.NotNull

data class NewOrder(
    @field:NotNull val paymentType: PaymentType,
    @field:NotNull val products: List<Product>,
    val instructions: String? = ""
)
