package com.andresexpress.orders.application.port.out;

import java.util.Optional;

/** Puerto de salida: donde quedan guardadas las guias en PDF. */
public interface WaybillDocumentPort {

    /** @return URL temporal para descargar el PDF, o vacio si todavia no se ha generado. */
    Optional<String> findDownloadUrl(String hashedTrackingNumber);
}
