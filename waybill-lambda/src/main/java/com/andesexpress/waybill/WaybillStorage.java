package com.andesexpress.waybill;

/** Puerto de salida: donde se guardan las guias en PDF. */
public interface WaybillStorage {
    void save(String key, byte[] pdf);
}
