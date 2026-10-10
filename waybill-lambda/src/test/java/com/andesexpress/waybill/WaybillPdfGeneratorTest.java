package com.andesexpress.waybill;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

import static com.andesexpress.waybill.Orders.anOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WaybillPdfGeneratorTest {

    private static final OffsetDateTime WHEN = OffsetDateTime.of(2026, 10, 9, 20, 15, 0, 0, WaybillPdfGenerator.COLOMBIA);

    private final WaybillPdfGenerator generator = new WaybillPdfGenerator();

    /** PDF sin comprimir: los textos quedan legibles dentro de los bytes. */
    private String textOf(OrderCreatedEvent order) {
        return new String(generator.generate(order, WHEN, false), StandardCharsets.ISO_8859_1);
    }

    private static OrderCreatedEvent with(String originDepartment, String destinationDepartment, String shipmentType,
                                         BigDecimal tariff, String senderName, String recipientName, String recipientPhone) {
        OrderCreatedEvent o = anOrder();
        return new OrderCreatedEvent(o.eventId(), o.plainTrackingNumber(), o.originCity(), originDepartment,
                o.destinationCity(), destinationDepartment, o.weight(), shipmentType, tariff,
                senderName, o.senderPhone(), recipientName, recipientPhone);
    }

    @Test
    @DisplayName("devuelve un PDF de una sola pagina")
    void returnsAOnePagePdf() {
        String pdf = new String(generator.generate(anOrder()), StandardCharsets.ISO_8859_1);

        assertThat(pdf).startsWith("%PDF-").endsWith("%%EOF\n");
        assertThat(pdf.split("/Type /Page ", -1)).hasSize(2);
    }

    @Test
    @DisplayName("RF-07: incluye todos los datos de la guia")
    void includesEveryWaybillField() {
        assertThat(textOf(anOrder())).contains("ANDES-12345678", "Medellín, Antioquia", "Rionegro, Santander",
                "3,5 kg", "Exprés", "$ 28.000 COP", "Ana Torres", "3001234567", "Luis Peña", "3007654321");
    }

    @Test
    @DisplayName("el tipo de envio estandar sale en espanol")
    void standardShipmentInSpanish() {
        OrderCreatedEvent order = with("Antioquia", "Santander", "STANDARD", new BigDecimal("28000"),
                "Ana Torres", "Luis Peña", "3007654321");

        assertThat(textOf(order)).contains("Estándar");
    }

    @Test
    @DisplayName("un pedido sin departamento muestra solo la ciudad")
    void orderWithoutDepartmentShowsOnlyTheCity() {
        OrderCreatedEvent order = with(null, null, "EXPRESS", new BigDecimal("28000"),
                "Ana Torres", "Luis Peña", "3007654321");

        assertThat(textOf(order)).contains("(Medellín)").doesNotContain("Medellín,");
    }

    @Test
    @DisplayName("sin tarifa muestra un guion")
    void missingTariffShowsDash() {
        OrderCreatedEvent order = with("Antioquia", "Santander", "EXPRESS", null,
                "Ana Torres", "Luis Peña", "3007654321");

        assertThat(textOf(order)).contains("(-)");
    }

    @Test
    @DisplayName("falta un dato obligatorio: se rechaza y dice cual")
    void missingRequiredFieldIsRejected() {
        OrderCreatedEvent order = with("Antioquia", "Santander", "EXPRESS", new BigDecimal("28000"),
                "Ana Torres", "Luis Peña", " ");

        assertThatThrownBy(() -> generator.generate(order))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("recipientPhone");
    }

    @Test
    @DisplayName("caracteres que la fuente no tiene no rompen el PDF")
    void unsupportedCharactersDoNotBreakThePdf() {
        OrderCreatedEvent order = with("Antioquia", "Santander", "EXPRESS", new BigDecimal("28000"),
                "Ana 😀 Torres", "Luis Peña", "3007654321");

        assertThat(textOf(order)).startsWith("%PDF-").contains("Ana ? Torres");
    }

    @Test
    @DisplayName("parentesis y barras del texto quedan escapados")
    void specialPdfCharactersAreEscaped() {
        OrderCreatedEvent order = with("Antioquia", "Santander", "EXPRESS", new BigDecimal("28000"),
                "Ana (Tienda) \\ Torres", "Luis Peña", "3007654321");

        assertThat(textOf(order)).contains("Ana \\(Tienda\\) \\\\ Torres");
    }

    @Test
    @DisplayName("un nombre muy largo se recorta con puntos suspensivos")
    void veryLongNameIsTruncated() {
        OrderCreatedEvent order = with("Antioquia", "Santander", "EXPRESS", new BigDecimal("28000"),
                "Ana Torres", "Nombre extremadamente largo ".repeat(8), "3007654321");

        assertThat(textOf(order)).contains("...)").doesNotContain("largo Nombre extremadamente largo Nombre extremadamente largo");
    }

    @Test
    @DisplayName("la fecha se muestra en hora de Colombia")
    void dateIsShownInColombianTime() {
        assertThat(textOf(anOrder())).contains("09/10/2026 20:15");
    }
}
