package com.andesexpress.waybill;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;

/**
 * Genera el PDF de la guia de transporte (RF-07). No depende de AWS ni de librerias de PDF:
 * escribe una pagina A5 con texto, lineas y rectangulos.
 */
public class WaybillPdfGenerator {

    /** Colombia no tiene horario de verano: UTC-5 fijo. */
    static final ZoneOffset COLOMBIA = ZoneOffset.ofHours(-5);

    private static final double PAGE_WIDTH = 148;   // A5 vertical, en mm
    private static final double PAGE_HEIGHT = 210;
    private static final double MARGIN = 10;
    private static final double LABEL_WIDTH = 34;
    private static final int MAX_VALUE_CHARS = 44;  // lo que cabe en una linea de la columna de valores

    private static final int[] NAVY = {18, 50, 90};
    private static final int[] GREY = {91, 102, 119};
    private static final int[] INK = {28, 35, 48};
    private static final int[] WHITE = {255, 255, 255};

    private static final DateTimeFormatter SHOWN_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generate(OrderCreatedEvent order) {
        return generate(order, OffsetDateTime.now(COLOMBIA), true);
    }

    /** @throws IllegalArgumentException si falta un dato obligatorio de la guia */
    byte[] generate(OrderCreatedEvent order, OffsetDateTime generatedAt, boolean compress) {
        List<String> missing = missingFields(order);
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Faltan datos para la guia: " + String.join(", ", missing));
        }

        String tracking = order.plainTrackingNumber().trim().toUpperCase(Locale.ROOT);
        Page page = new Page();

        page.rect(0, 0, PAGE_WIDTH, 24, NAVY);
        page.text(MARGIN, 13, "ANDES EXPRESS", 17, WHITE, true);
        page.text(MARGIN, 19, "Guía de transporte", 9, WHITE, false);

        page.text(MARGIN, 34, "NÚMERO DE GUÍA", 8, GREY, false);
        page.text(MARGIN, 43, tracking, 22, INK, true);

        Map<String, String> shipment = new LinkedHashMap<>();
        shipment.put("Origen", place(order.originCity(), order.originDepartment()));
        shipment.put("Destino", place(order.destinationCity(), order.destinationDepartment()));
        shipment.put("Peso", weight(order.weight()));
        shipment.put("Tipo de envío", shipmentType(order.shipmentType()));
        shipment.put("Tarifa", money(order.totalTariff()));

        Map<String, String> sender = new LinkedHashMap<>();
        sender.put("Nombre", order.senderName());
        sender.put("Teléfono", order.senderPhone());

        Map<String, String> recipient = new LinkedHashMap<>();
        recipient.put("Nombre", order.recipientName());
        recipient.put("Teléfono", order.recipientPhone());

        double y = section(page, 54, "Envío", shipment);
        y = section(page, y + 5, "Remitente", sender);
        section(page, y + 5, "Destinatario", recipient);

        page.text(MARGIN, 198, "Documento generado el " + generatedAt.format(SHOWN_DATE) + " (hora de Colombia).",
                7.5, GREY, false);
        page.text(MARGIN, 202, "Consulte su guía en la plataforma con el número indicado arriba.", 7.5, GREY, false);

        return page.toPdf("Guía " + tracking, generatedAt, compress);
    }

    private static List<String> missingFields(OrderCreatedEvent order) {
        Map<String, Object> required = new LinkedHashMap<>();
        required.put("plainTrackingNumber", order.plainTrackingNumber());
        required.put("originCity", order.originCity());
        required.put("destinationCity", order.destinationCity());
        required.put("weight", order.weight());
        required.put("shipmentType", order.shipmentType());
        required.put("senderName", order.senderName());
        required.put("senderPhone", order.senderPhone());
        required.put("recipientName", order.recipientName());
        required.put("recipientPhone", order.recipientPhone());

        List<String> missing = new ArrayList<>();
        required.forEach((name, value) -> {
            if (value == null || (value instanceof String text && text.isBlank())) {
                missing.add(name);
            }
        });
        return missing;
    }

    private static double section(Page page, double y, String title, Map<String, String> rows) {
        page.text(MARGIN, y, title, 10.5, NAVY, true);
        y += 2.5;
        page.line(MARGIN, y, PAGE_WIDTH - MARGIN, y, NAVY, 0.4);
        y += 6.5;
        for (Map.Entry<String, String> row : rows.entrySet()) {
            page.text(MARGIN, y, row.getKey(), 9, GREY, false);
            page.text(MARGIN + LABEL_WIDTH, y, fit(row.getValue()), 10, INK, true);
            y += 6.5;
        }
        return y;
    }

    private static String place(String city, String department) {
        return department == null || department.isBlank() ? city : city + ", " + department;
    }

    private static String weight(Double kg) {
        return BigDecimal.valueOf(kg).stripTrailingZeros().toPlainString().replace('.', ',') + " kg";
    }

    private static String shipmentType(String type) {
        return switch (type.toUpperCase(Locale.ROOT)) {
            case "STANDARD" -> "Estándar";
            case "EXPRESS" -> "Exprés";
            default -> type;
        };
    }

    private static String money(BigDecimal value) {
        if (value == null) {
            return "-";
        }
        return "$ " + String.format(Locale.ROOT, "%,d", Math.round(value.doubleValue())).replace(',', '.') + " COP";
    }

    /** Recorta con "..." los textos que no caben en una linea. */
    private static String fit(String text) {
        return text.length() <= MAX_VALUE_CHARS ? text : text.substring(0, MAX_VALUE_CHARS - 3).stripTrailing() + "...";
    }

    /** Una pagina A5. Coordenadas en mm desde la esquina superior izquierda. */
    private static final class Page {

        private static final double MM = 72 / 25.4; // puntos PDF por milimetro
        /** WinAnsi incluye tildes y la ñ; lo que no exista ahi sale como "?". */
        private static final Charset WIN_ANSI = Charset.forName("windows-1252");

        private final ByteArrayOutputStream content = new ByteArrayOutputStream();

        void rect(double x, double y, double width, double height, int[] rgb) {
            op("%s rg %.2f %.2f %.2f %.2f re f\n", color(rgb), x * MM, (PAGE_HEIGHT - y - height) * MM, width * MM, height * MM);
        }

        void line(double x1, double y1, double x2, double y2, int[] rgb, double width) {
            op("%s RG %.2f w %.2f %.2f m %.2f %.2f l S\n", color(rgb), width * MM,
                    x1 * MM, (PAGE_HEIGHT - y1) * MM, x2 * MM, (PAGE_HEIGHT - y2) * MM);
        }

        /** y es la linea base del texto. */
        void text(double x, double y, String text, double size, int[] rgb, boolean bold) {
            op("BT /F%d %.1f Tf %s rg %.2f %.2f Td (", bold ? 2 : 1, size, color(rgb), x * MM, (PAGE_HEIGHT - y) * MM);
            content.writeBytes(pdfString(text));
            op(") Tj ET\n");
        }

        byte[] toPdf(String title, OffsetDateTime created, boolean compress) {
            byte[] stream = compress ? deflate(content.toByteArray()) : content.toByteArray();
            String streamDict = "<< /Length " + stream.length + (compress ? " /Filter /FlateDecode" : "") + " >>";

            List<byte[]> objects = List.of(
                    ascii("<< /Type /Catalog /Pages 2 0 R >>"),
                    ascii("<< /Type /Pages /Kids [3 0 R] /Count 1 >>"),
                    ascii(String.format(Locale.ROOT, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 %.2f %.2f] /Contents 6 0 R "
                            + "/Resources << /Font << /F1 4 0 R /F2 5 0 R >> >> >>", PAGE_WIDTH * MM, PAGE_HEIGHT * MM)),
                    ascii("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>"),
                    ascii("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>"),
                    join(ascii(streamDict + "\nstream\n"), stream, ascii("\nendstream")),
                    join(ascii("<< /Title ("), pdfString(title), ascii(") /Author (Andes Express) /CreationDate (D:"
                            + created.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "-05'00') >>")));

            ByteArrayOutputStream pdf = new ByteArrayOutputStream();
            pdf.writeBytes(new byte[] {'%', 'P', 'D', 'F', '-', '1', '.', '4', '\n',
                    '%', (byte) 0xE2, (byte) 0xE3, (byte) 0xCF, (byte) 0xD3, '\n'});
            List<Integer> offsets = new ArrayList<>();
            for (int i = 0; i < objects.size(); i++) {
                offsets.add(pdf.size());
                pdf.writeBytes(ascii((i + 1) + " 0 obj\n"));
                pdf.writeBytes(objects.get(i));
                pdf.writeBytes(ascii("\nendobj\n"));
            }
            int xrefAt = pdf.size();
            pdf.writeBytes(ascii("xref\n0 " + (objects.size() + 1) + "\n0000000000 65535 f \n"));
            for (int offset : offsets) {
                pdf.writeBytes(ascii(String.format(Locale.ROOT, "%010d 00000 n \n", offset)));
            }
            pdf.writeBytes(ascii("trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R /Info 7 0 R >>\nstartxref\n"
                    + xrefAt + "\n%%EOF\n"));
            return pdf.toByteArray();
        }

        private void op(String format, Object... args) {
            content.writeBytes(ascii(String.format(Locale.ROOT, format, args)));
        }

        private static String color(int[] rgb) {
            return String.format(Locale.ROOT, "%.3f %.3f %.3f", rgb[0] / 255.0, rgb[1] / 255.0, rgb[2] / 255.0);
        }

        private static byte[] pdfString(String text) {
            ByteArrayOutputStream escaped = new ByteArrayOutputStream();
            for (byte b : text.getBytes(WIN_ANSI)) {
                if (b == '\\' || b == '(' || b == ')') {
                    escaped.write('\\');
                }
                escaped.write(b);
            }
            return escaped.toByteArray();
        }

        private static byte[] deflate(byte[] data) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (DeflaterOutputStream deflater = new DeflaterOutputStream(out)) {
                deflater.write(data);
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
            return out.toByteArray();
        }

        private static byte[] ascii(String text) {
            return text.getBytes(StandardCharsets.US_ASCII);
        }

        private static byte[] join(byte[]... parts) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (byte[] part : parts) {
                out.writeBytes(part);
            }
            return out.toByteArray();
        }
    }
}
