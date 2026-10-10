package com.andresexpress.orders.application.service;

import com.andresexpress.orders.application.port.in.GetWaybillUseCase;
import com.andresexpress.orders.application.port.out.WaybillDocumentPort;
import com.andresexpress.orders.application.port.out.WaybillRequestPort;
import com.andresexpress.orders.domain.exception.OrderNotFoundException;
import com.andresexpress.orders.domain.exception.WaybillPdfNotReadyException;
import com.andresexpress.orders.domain.model.OrderDomain;
import com.andresexpress.orders.domain.model.ShipmentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WaybillPdfServicesTest {

    private static final String TRACKING = "ANDES-12345678";
    private static final String HASH = OrderDomain.calculateHash(TRACKING);

    @Mock
    private GetWaybillUseCase getWaybillUseCase;
    @Mock
    private WaybillDocumentPort waybillDocumentPort;
    @Mock
    private WaybillRequestPort waybillRequestPort;

    /** Pedido tal como sale de la base de datos: con el hash, sin el numero en claro (RN-13). */
    private static OrderDomain storedOrder() {
        return OrderDomain.builder()
                .hashedTrackingNumber(HASH)
                .originCity("Medellín").originDepartment("Antioquia")
                .destinationCity("Rionegro").destinationDepartment("Santander")
                .weight(3.5).shipmentType(ShipmentType.EXPRESS)
                .senderName("Ana Torres").recipientName("Luis Peña")
                .build();
    }

    @Nested
    @DisplayName("RF-07: obtener el enlace de la guia en PDF")
    class GetDownloadUrl {

        private GetWaybillPdfService service() {
            return new GetWaybillPdfService(getWaybillUseCase, waybillDocumentPort);
        }

        @Test
        @DisplayName("devuelve la URL del PDF buscandolo por el hash de la guia")
        void returnsTheUrl() {
            when(getWaybillUseCase.getWaybillByTrackingNumber(TRACKING)).thenReturn(storedOrder());
            when(waybillDocumentPort.findDownloadUrl(HASH)).thenReturn(Optional.of("https://bucket/guia.pdf?firma"));

            assertThat(service().getDownloadUrl(TRACKING)).isEqualTo("https://bucket/guia.pdf?firma");
        }

        @Test
        @DisplayName("el pedido existe pero el PDF aun no: avisa que no esta listo")
        void pdfNotReadyYet() {
            when(getWaybillUseCase.getWaybillByTrackingNumber(TRACKING)).thenReturn(storedOrder());
            when(waybillDocumentPort.findDownloadUrl(HASH)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service().getDownloadUrl(TRACKING))
                    .isInstanceOf(WaybillPdfNotReadyException.class)
                    .hasMessageContaining("todavía no está lista");
        }

        @Test
        @DisplayName("una guia que no existe no llega a consultar el almacenamiento")
        void unknownWaybillDoesNotTouchStorage() {
            when(getWaybillUseCase.getWaybillByTrackingNumber("ANDES-00000000"))
                    .thenThrow(new OrderNotFoundException("No existe una guía con el número ANDES-00000000"));

            assertThatThrownBy(() -> service().getDownloadUrl("ANDES-00000000")).isInstanceOf(OrderNotFoundException.class);

            verifyNoInteractions(waybillDocumentPort);
        }
    }

    @Nested
    @DisplayName("RF-09: regenerar la guia en PDF")
    class Regenerate {

        private RegenerateWaybillService service() {
            return new RegenerateWaybillService(getWaybillUseCase, waybillRequestPort);
        }

        @Test
        @DisplayName("vuelve a pedir la guia con los datos guardados y el numero que escribio el cliente")
        void requestsTheWaybillAgain() {
            when(getWaybillUseCase.getWaybillByTrackingNumber("  andes-12345678 ")).thenReturn(storedOrder());

            service().regenerate("  andes-12345678 ");

            ArgumentCaptor<OrderDomain> requested = ArgumentCaptor.forClass(OrderDomain.class);
            verify(waybillRequestPort).requestWaybill(requested.capture());
            assertThat(requested.getValue().getPlainTrackingNumber()).isEqualTo(TRACKING);
            assertThat(requested.getValue().getHashedTrackingNumber()).isEqualTo(HASH);
            assertThat(requested.getValue().getDestinationDepartment()).isEqualTo("Santander");
            assertThat(requested.getValue().getRecipientName()).isEqualTo("Luis Peña");
        }

        @Test
        @DisplayName("una guia que no existe no pide nada")
        void unknownWaybillRequestsNothing() {
            when(getWaybillUseCase.getWaybillByTrackingNumber("ANDES-00000000"))
                    .thenThrow(new OrderNotFoundException("No existe una guía con el número ANDES-00000000"));

            assertThatThrownBy(() -> service().regenerate("ANDES-00000000")).isInstanceOf(OrderNotFoundException.class);

            verifyNoInteractions(waybillRequestPort);
        }
    }
}
