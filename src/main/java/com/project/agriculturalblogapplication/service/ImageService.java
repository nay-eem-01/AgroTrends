package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.storage.ImageStorage;
import com.project.agriculturalblogapplication.storage.ImageType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ImageService {

    public static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;

    private final ImageStorage imageStorage;

    /** Images for posts and questions: JPEG/PNG/WebP up to 5 MB. Returns the public URL. */
    public String upload(byte[] bytes, String lang) {
        if (bytes == null || bytes.length == 0) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_IMAGE_REQUIRED, lang);
        }
        if (bytes.length > MAX_IMAGE_BYTES) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_IMAGE_TOO_LARGE, lang);
        }
        ImageType type = ImageType.detect(bytes).orElseThrow(() ->
                new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_IMAGE_TYPE_NOT_ALLOWED, lang));
        return imageStorage.store(bytes, type);
    }

    /** Returns {@code urls} unchanged when every one was uploaded here; 400 otherwise. */
    public List<String> requireUploaded(List<String> urls, String lang) {
        if (!urls.stream().allMatch(imageStorage::isStored)) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_IMAGE_NOT_UPLOADED, lang);
        }
        return urls;
    }
}
