package com.project.agriculturalblogapplication.model;

import com.project.agriculturalblogapplication.entities.AgriMetadata;
import com.project.agriculturalblogapplication.enums.CropSeason;
import com.project.agriculturalblogapplication.enums.SoilType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AgriInfoTest {

    @Test
    void cropAndRegionAreStoredLowercaseAndBlankMeansUnset() {
        AgriMetadata metadata = new AgriInfo("  Boro   Rice ", CropSeason.RABI, " ", SoilType.CLAY_LOAM).toMetadata();

        assertEquals("boro rice", metadata.getCrop());
        assertNull(metadata.getRegion());
        assertEquals(CropSeason.RABI, metadata.getSeason());
        assertEquals(new AgriInfo("boro rice", CropSeason.RABI, null, SoilType.CLAY_LOAM), AgriInfo.from(metadata));
    }

    @Test
    void aMissingEmbeddableReadsAsAllUnset() {
        assertEquals(new AgriInfo(null, null, null, null), AgriInfo.from(null));
    }
}
