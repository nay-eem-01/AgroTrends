package com.project.agriculturalblogapplication.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConfigurationProperties(prefix = "app.storage")
@Getter
@Setter
public class StorageProperties {

    /** Directory for uploaded images (local-disk storage). */
    private String localDir = "uploads";

    /** Public URL prefix the images are served under, e.g. https://api.example.com/uploads */
    private String publicBaseUrl = "http://localhost:8080/uploads";

    /** Cloudflare R2; a blank bucket keeps images on the local disk. */
    private R2 r2 = new R2();

    @Getter
    @Setter
    public static class R2 {
        /** https://<account id>.r2.cloudflarestorage.com */
        private String endpoint;
        private String bucket;
        private String accessKeyId;
        private String secretAccessKey;
    }

    /** The name is generated, never taken from the upload, so no path tricks are possible. */
    static String newFileName(ImageType type) {
        return UUID.randomUUID() + "." + type.extension();
    }

    String publicUrl(String fileName) {
        return baseUrl() + "/" + fileName;
    }

    /** True for a URL of the form {@link #publicUrl} returns, so posts never embed images from other hosts. */
    boolean isPublicUrl(String url) {
        String prefix = baseUrl() + "/";
        return url != null && url.startsWith(prefix) && url.substring(prefix.length()).matches("[0-9a-f-]+\\.[a-z]+");
    }

    private String baseUrl() {
        return publicBaseUrl.replaceAll("/+$", "");
    }
}
