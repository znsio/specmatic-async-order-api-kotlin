package com.component.orders.models

import javax.validation.constraints.NotNull
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class Product @JsonCreator constructor(
    @JsonProperty("id") @field:NotNull val id: Int,
    @JsonProperty("quantity") @field:NotNull val quantity: Int
)
