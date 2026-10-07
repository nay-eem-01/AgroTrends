package com.project.agriculturalblogapplication.storage;

import java.util.Arrays;
import java.util.Optional;

/** Allowed image formats, recognised by their first bytes - never by the client's Content-Type or file name. */
public enum ImageType {
    JPEG("jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
    PNG("png", new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'}),
    WEBP("webp", null);

    private final String extension;
    private final byte[] signature;

    ImageType(String extension, byte[] signature) {
        this.extension = extension;
        this.signature = signature;
    }

    public String extension() {
        return extension;
    }

    public static Optional<ImageType> detect(byte[] bytes) {
        if (bytes == null) {
            return Optional.empty();
        }
        // WebP: "RIFF" <4-byte size> "WEBP"
        if (bytes.length >= 12 && startsWith(bytes, 0, "RIFF".getBytes()) && startsWith(bytes, 8, "WEBP".getBytes())) {
            return Optional.of(WEBP);
        }
        return Arrays.stream(values())
                .filter(type -> type.signature != null && startsWith(bytes, 0, type.signature))
                .findFirst();
    }

    private static boolean startsWith(byte[] bytes, int offset, byte[] prefix) {
        if (bytes.length < offset + prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[offset + i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
