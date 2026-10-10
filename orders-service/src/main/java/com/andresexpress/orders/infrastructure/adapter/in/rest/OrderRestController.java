package com.andresexpress.orders.infrastructure.adapter.in.rest;

import com.andresexpress.orders.application.port.in.CreateOrderUseCase;
import com.andresexpress.orders.application.port.in.GetWaybillPdfUseCase;
import com.andresexpress.orders.application.port.in.GetWaybillUseCase;
import com.andresexpress.orders.application.port.in.RegenerateWaybillUseCase;
import com.andresexpress.orders.domain.model.OrderDomain;
import com.andresexpress.orders.infrastructure.adapter.in.rest.dto.CreateOrderRequest;
import com.andresexpress.orders.infrastructure.adapter.in.rest.dto.OrderCreatedResponse;
import com.andresexpress.orders.infrastructure.adapter.in.rest.dto.WaybillPdfResponse;
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
    private final GetWaybillPdfUseCase getWaybillPdfUseCase;
    private final RegenerateWaybillUseCase regenerateWaybillUseCase;

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

    /** RF-07: enlace temporal para descargar la guia en PDF. 404 si la guia no existe o el PDF aun no esta listo. */
    @GetMapping("/guides/{guideNumber}/pdf")
    public ResponseEntity<WaybillPdfResponse> getWaybillPdf(@PathVariable String guideNumber) {
        String url = getWaybillPdfUseCase.getDownloadUrl(guideNumber);
        return ResponseEntity.ok(new WaybillPdfResponse(normalized(guideNumber), url,
                "Enlace de descarga temporal."));
    }

    /** RF-09: vuelve a pedir la generacion del PDF. Responde 202 porque se genera en segundo plano. */
    @PostMapping("/guides/{guideNumber}/pdf")
    public ResponseEntity<WaybillPdfResponse> regenerateWaybillPdf(@PathVariable String guideNumber) {
        regenerateWaybillUseCase.regenerate(guideNumber);
        return ResponseEntity.accepted().body(new WaybillPdfResponse(normalized(guideNumber), null,
                "Se solicitó generar de nuevo la guía en PDF. Estará disponible en unos segundos."));
    }

    private static String normalized(String guideNumber) {
        return guideNumber.trim().toUpperCase();
    }
}
