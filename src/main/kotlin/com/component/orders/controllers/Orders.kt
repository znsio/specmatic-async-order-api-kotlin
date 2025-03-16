package com.component.orders.controllers

import com.component.orders.models.Id
import com.component.orders.services.OrderService
import com.component.orders.models.NewOrder
import com.component.orders.models.Order
import com.component.orders.models.OrderStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@RestController
@Validated
@RequestMapping("/orders")
class Orders(@Autowired val orderService: OrderService) {
    @GetMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getOrder(
        @PathVariable(
            name = "id",
            required = true,
        ) id: Int,
    ): ResponseEntity<Order> {
        return ResponseEntity(orderService.get(id), HttpStatus.OK)
    }

    @PostMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun createOrder(
        @Valid @RequestBody newOrder: NewOrder
    ): ResponseEntity<Id> = ResponseEntity(orderService.create(newOrder), HttpStatus.ACCEPTED)
}