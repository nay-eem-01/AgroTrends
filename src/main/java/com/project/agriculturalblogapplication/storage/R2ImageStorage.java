package com.project.agriculturalblogapplication.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;

/**
 * Images in a Cloudflare R2 bucket, served from its public URL (r2.dev or a custom domain) as
 * {@code app.storage.public-base-url}. Used when {@code app.storage.r2.bucket} is set.
 */
@Component
@ConditionalOnExpression("!'${app.storage.r2.bucket:}'.isEmpty()")
public class R2ImageStorage implements ImageStorage {

    private final StorageProperties storageProperties;
    private final S3Client s3;

    @Autowired
    public R2ImageStorage(StorageProperties storageProperties) {
        this(storageProperties, client(storageProperties.getR2()));
    }

    R2ImageStorage(StorageProperties storageProperties, S3Client s3) {
        this.storageProperties = storageProperties;
        this.s3 = s3;
    }

    private static S3Client client(StorageProperties.R2 r2) {
        return S3Client.builder()
                .endpointOverride(URI.create(r2.getEndpoint()))
                .region(Region.of("auto")) // R2 ignores the region, the SDK requires one
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(r2.getAccessKeyId(), r2.getSecretAccessKey())))
                .build();
    }

    @Override
    public String store(byte[] bytes, ImageType type) {
        String fileName = StorageProperties.newFileName(type);
        s3.putObject(PutObjectRequest.builder()
                        .bucket(storageProperties.getR2().getBucket())
                        .key(fileName)
                        .contentType(type.contentType())
                        // Names are never reused, so browsers and the CDN may keep a copy for a year.
                        .cacheControl("public, max-age=31536000, immutable")
                        .build(),
                RequestBody.fromBytes(bytes));
        return storageProperties.publicUrl(fileName);
    }

    @Override
    public boolean isStored(String url) {
        return storageProperties.isPublicUrl(url);
    }
}
