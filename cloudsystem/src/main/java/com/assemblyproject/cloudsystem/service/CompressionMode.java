package com.assemblyproject.cloudsystem.service;

import java.util.Locale;

/**
 * Escolha de compressão feita pelo utilizador no momento do upload (RF09).
 * <p>
 * O requisito funcional prevê compactação automática, mas a aplicação expõe
 * também a possibilidade de guardar o ficheiro sem compressão, para o
 * utilizador controlar espaço usado vs. tempo de transferência.
 */
public enum CompressionMode {

    /** Guarda os bytes originais, sem compactação. */
    NONE,

    /** O sistema decide: compacta se valer a pena e salta se já estiver compactado. */
    AUTO,

    /** Força DEFLATE (zlib). */
    DEFLATE,

    /** Força GZIP. */
    GZIP;

    public static CompressionMode parse(String value) {
        if (value == null || value.isBlank()) {
            return AUTO;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return AUTO;
        }
    }
}
