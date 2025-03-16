package com.component.orders.models

import javax.validation.constraints.NotNull

data class Product(
    @field:NotNull val id: Int,
    @field:NotNull val quantity: Int
)
