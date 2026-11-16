package com.project.agriculturalblogapplication.storage;

/**
 * Where uploaded images live. The local-disk implementation is for one server; an object store (S3, GCS) can
 * replace it without touching callers.
 */
public interface ImageStorage {

    /** Stores the bytes under a new random name and returns the public URL to embed in a post. */
    String store(byte[] bytes, ImageType type);

    /** Deletes only a URL belonging to this storage; missing files are harmless. */
    void delete(String url);

    /** True when {@code url} is one that {@link #store} returned, so posts never embed images from other hosts. */
    boolean isStored(String url);
}
