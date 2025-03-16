package com.component.orders.models

data class Order(val id: Int, val paymentType: PaymentType, val products: List<Product>, val status: OrderStatus, val instructions:String?="")