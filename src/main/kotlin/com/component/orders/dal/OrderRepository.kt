package com.component.orders.dal

import com.component.orders.models.Order
import org.springframework.data.jpa.repository.JpaRepository

internal interface OrderRepository: JpaRepository<Order, Int>