package com.andresexpress.orders.infrastructure.adapter.in.rest.dto;

/**
 * @param url     enlace temporal para descargar la guia en PDF; nulo cuando solo se solicito generarla
 * @param message explicacion para mostrarle al cliente
 */
public record WaybillPdfResponse(String trackingNumber, String url, String message) {
}
