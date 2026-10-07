package com.project.agriculturalblogapplication.entities;

import com.project.agriculturalblogapplication.enums.CropSeason;
import com.project.agriculturalblogapplication.enums.SoilType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** What a post or question is about agriculturally; every field is optional. Crop and region are lowercase. */
@Embeddable
@NoArgsConstructor
@Getter
@Setter
public class AgriMetadata {

    @Column(name = "agri_crop")
    private String crop;

    @Enumerated(EnumType.STRING)
    @Column(name = "agri_season")
    private CropSeason season;

    @Column(name = "agri_region")
    private String region;

    @Enumerated(EnumType.STRING)
    @Column(name = "agri_soil")
    private SoilType soil;
}
