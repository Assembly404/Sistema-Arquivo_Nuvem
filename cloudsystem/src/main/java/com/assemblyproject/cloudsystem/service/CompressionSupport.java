package com.assemblyproject.cloudsystem.service;

import java.util.Locale;
import java.util.Set;

/**
 * Regras de negócio da compressão (RF09, HU 2.1):
 * <ul>
 *   <li>Formatos já compactados não são recomprimidos (perda de tempo sem ganho).</li>
 *   <li>Compensa só compactar quando há um ganho mínimo real.</li>
 * </ul>
 */
public final class CompressionSupport {

    /** Ganho mínimo (5%) para valer a pena guardar comprimido. */
    private static final double MIN_GAIN = 0.05;

    private static final Set<String> COMPRESSED_EXTENSIONS = Set.of(
            "zip", "rar", "7z", "gz", "gzip", "bz2", "xz", "zst", "tgz", "tar.gz",
            "jpg", "jpeg", "png", "gif", "webp", "heic", "avif", "bmp",
            "mp3", "aac", "ogg", "opus", "flac", "m4a", "wma",
            "mp4", "mkv", "avi", "mov", "webm", "m4v",
            "pdf", "docx", "xlsx", "pptx", "odt", "ods", "odp",
            "apk", "jar", "war", "deb", "rpm", "iso", "dmg");

    private static final Set<String> COMPRESSED_MIME_HINTS = Set.of(
            "zip", "rar", "7z-compressed", "gzip", "x-bzip2", "xz", "zstd",
            "jpeg", "png", "gif", "webp", "avif", "bmp",
            "mpeg", "mp4", "webm", "ogg", "quicktime", "x-msvideo",
            "pdf", "officedocument", "opendocument");

    private CompressionSupport() {
    }

    /**
     * Deteta se o ficheiro já está compactado e, por isso, não deve ser
     * recomprimido (HU 2.1 — critério de aceite do .zip).
     */
    public static boolean isAlreadyCompressed(String originalName, String mimeType) {
        if (originalName != null) {
            String lower = originalName.toLowerCase(Locale.ROOT);
            int dot = lower.lastIndexOf('.');
            if (dot >= 0 && dot < lower.length() - 1) {
                String ext = lower.substring(dot + 1);
                if (COMPRESSED_EXTENSIONS.contains(ext)) {
                    return true;
                }
            }
        }
        if (mimeType != null) {
            String lower = mimeType.toLowerCase(Locale.ROOT);
            for (String hint : COMPRESSED_MIME_HINTS) {
                if (lower.contains(hint)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** {@code true} se a versão comprimida valer a pena face à original. */
    public static boolean isBeneficial(long originalSize, long compressedSize) {
        if (originalSize <= 0) {
            return false;
        }
        double ratio = (double) compressedSize / originalSize;
        return ratio <= (1.0 - MIN_GAIN);
    }
}