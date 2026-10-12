package com.assemblyproject.cloudsystem.service;

/**
 * Algoritmos de compressão suportados pela extensão própria {@code .dbs}
 * (RF09/RF10). Cada valor tem um código de 1 byte gravado no cabeçalho do
 * container, garantindo que o download sabe sempre como descompactar,
 * independentemente de o utilizador ter escolhido compressão ou não.
 */
public enum CompressionAlgorithm {

    /** Sem compressão (armazenamento direto). */
    NONE(0),

    /** DEFLATE (RFC 1951) / zlib, o mesmo usado pelo formato .zip. */
    DEFLATE(1),

    /** GZIP (RFC 1952), DEFLATE com cabeçalho e CRC. */
    GZIP(2);

    private final int code;

    CompressionAlgorithm(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static CompressionAlgorithm fromCode(int code) {
        for (CompressionAlgorithm algorithm : values()) {
            if (algorithm.code == code) {
                return algorithm;
            }
        }
        throw new IllegalArgumentException("Algoritmo de compressão desconhecido: " + code);
    }
}
