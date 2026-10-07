package com.project.agriculturalblogapplication.repositories;

import com.project.agriculturalblogapplication.model.AgriInfo;
import org.springframework.data.jpa.domain.Specification;

/** Optional equality filters on an entity's embedded {@code agri} field; unset filters match everything. */
public final class AgriSpecifications {

    private AgriSpecifications() {}

    public static <T> Specification<T> matches(AgriInfo filter) {
        Specification<T> spec = (root, query, cb) -> cb.conjunction();
        if (filter == null) {
            return spec;
        }
        String crop = AgriInfo.normalize(filter.crop());
        String region = AgriInfo.normalize(filter.region());
        if (crop != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("agri").get("crop"), crop));
        }
        if (filter.season() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("agri").get("season"), filter.season()));
        }
        if (region != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("agri").get("region"), region));
        }
        if (filter.soil() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("agri").get("soil"), filter.soil()));
        }
        return spec;
    }
}
