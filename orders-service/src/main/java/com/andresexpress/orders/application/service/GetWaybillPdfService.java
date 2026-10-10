package com.andresexpress.orders.application.service;

import com.andresexpress.orders.application.port.in.GetWaybillPdfUseCase;
import com.andresexpress.orders.application.port.in.GetWaybillUseCase;
import com.andresexpress.orders.application.port.out.WaybillDocumentPort;
import com.andresexpress.orders.domain.exception.WaybillPdfNotReadyException;
import com.andresexpress.orders.domain.model.OrderDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetWaybillPdfService implements GetWaybillPdfUseCase {

    private final GetWaybillUseCase getWaybillUseCase;
    private final WaybillDocumentPort waybillDocumentPort;

    @Override
    public String getDownloadUrl(String trackingNumber) {
        // Primero se confirma que la guia existe: un numero inventado responde 404 sin consultar el almacenamiento
        OrderDomain order = getWaybillUseCase.getWaybillByTrackingNumber(trackingNumber);

        return waybillDocumentPort.findDownloadUrl(order.getHashedTrackingNumber())
                .orElseThrow(() -> new WaybillPdfNotReadyException(
                        "La guía en PDF todavía no está lista. Intente de nuevo en unos segundos o solicite regenerarla."));
    }
}
