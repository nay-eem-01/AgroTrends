package com.project.agriculturalblogapplication.model;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.entities.AgriMetadata;
import com.project.agriculturalblogapplication.enums.CropSeason;
import com.project.agriculturalblogapplication.enums.SoilType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.Locale;

/** Agricultural context of a post or question, used in requests, responses and list filters. */
public record AgriInfo(
        @Schema(example = "rice") @Size(max = 60, message = ErrorCode.ERROR_AGRI_FIELD_TOO_LONG) String crop,
        @Schema(example = "RABI") CropSeason season,
        @Schema(example = "rangpur") @Size(max = 60, message = ErrorCode.ERROR_AGRI_FIELD_TOO_LONG) String region,
        @Schema(example = "CLAY_LOAM") SoilType soil
) {

    public static AgriInfo from(AgriMetadata metadata) {
        return metadata == null ? new AgriInfo(null, null, null, null)
                : new AgriInfo(metadata.getCrop(), metadata.getSeason(), metadata.getRegion(), metadata.getSoil());
    }

    public AgriMetadata toMetadata() {
        AgriMetadata metadata = new AgriMetadata();
        metadata.setCrop(normalize(crop));
        metadata.setSeason(season);
        metadata.setRegion(normalize(region));
        metadata.setSoil(soil);
        return metadata;
    }

    /** "  Rice " and "rice" filter the same; blank means "not set". */
    public static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
