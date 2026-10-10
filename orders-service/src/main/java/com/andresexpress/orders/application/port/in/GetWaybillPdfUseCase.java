package com.andresexpress.orders.application.port.in;

public interface GetWaybillPdfUseCase {

    /** @return URL temporal para descargar la guia en PDF. */
    String getDownloadUrl(String trackingNumber);
}
