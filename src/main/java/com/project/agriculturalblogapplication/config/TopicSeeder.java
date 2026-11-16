package com.project.agriculturalblogapplication.config;

import com.project.agriculturalblogapplication.entities.Category;
import com.project.agriculturalblogapplication.repositories.CategoryRepositories;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TopicSeeder {
    private final CategoryRepositories categories;

    static final List<String> TOPICS = List.of(
            "Rice", "Wheat and maize", "Pulses", "Oilseeds", "Vegetables", "Fruits", "Spices",
            "Flowers and ornamentals", "Jute and fibre crops", "Tea and plantation crops", "Mushrooms",
            "Seeds and nurseries", "Soil health", "Fertilizers and plant nutrition", "Composting",
            "Irrigation and water management", "Plant diseases", "Insect pests", "Weed management",
            "Integrated pest management", "Organic farming", "Climate-smart agriculture",
            "Flood, drought and salinity", "Farm machinery", "Harvest and storage", "Food processing and safety",
            "Cattle and dairy", "Goats and sheep", "Poultry", "Livestock health and nutrition",
            "Freshwater fish farming", "Shrimp and prawn farming", "Fish health and pond management",
            "Beekeeping", "Agroforestry", "Rooftop and home gardening", "Agribusiness and markets",
            "Farm finance and cooperatives", "Agricultural education and careers", "General agriculture");

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        if (categories.count() != 0) return;
        categories.saveAll(TOPICS.stream().map(name -> {
            Category category = new Category();
            category.setCategoryName(name);
            return category;
        }).toList());
    }
}
