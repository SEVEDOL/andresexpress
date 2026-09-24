package com.andresexpress.orders.infrastructure.adapter.in.rest;

import com.andresexpress.orders.application.port.in.CreateOrderUseCase;
import com.andresexpress.orders.application.port.in.GetWaybillUseCase;
import com.andresexpress.orders.domain.model.OrderDomain;
import com.andresexpress.orders.infrastructure.adapter.in.rest.dto.CreateOrderRequest;
import com.andresexpress.orders.infrastructure.adapter.in.rest.dto.OrderCreatedResponse;
import com.andresexpress.orders.infrastructure.adapter.in.rest.dto.WaybillResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderRestController {

    private final CreateOrderUseCase createOrderUseCase;
    private final GetWaybillUseCase getWaybillUseCase;

    @PostMapping
    public ResponseEntity<OrderCreatedResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderDomain order = createOrderUseCase.createOrder(OrderRestMapper.toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderRestMapper.toCreatedResponse(order));
    }

    @GetMapping("/guides/{guideNumber}")
    public ResponseEntity<WaybillResponse> getWaybill(@PathVariable String guideNumber) {
        OrderDomain order = getWaybillUseCase.getWaybillByTrackingNumber(guideNumber);
        return ResponseEntity.ok(OrderRestMapper.toWaybillResponse(order, guideNumber));
    }
}
