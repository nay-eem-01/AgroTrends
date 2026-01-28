package com.project.agriculturalblogapplication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class AgriculturalBlogApplication {

	public static void main(String[] args) {
		SpringApplication.run(AgriculturalBlogApplication.class, args);
	}
}
