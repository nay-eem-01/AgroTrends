package com.project.agriculturalblogapplication;

import com.project.agriculturalblogapplication.Models.APP_ROLE;
import com.project.agriculturalblogapplication.Models.Roles;
import com.project.agriculturalblogapplication.Repositories.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AgriculturalBlogApplication {

	public static void main(String[] args) {
		SpringApplication.run(AgriculturalBlogApplication.class, args);

	}

	@Bean
	public CommandLineRunner setupDefaultRoles(RoleRepository roleRepository) {
		return args -> {
			if (roleRepository.findByRole(APP_ROLE.ROLE_USER).isEmpty()) {
				roleRepository.save(new Roles(APP_ROLE.ROLE_USER));
			}
			if (roleRepository.findByRole(APP_ROLE.ROLE_ADMIN).isEmpty()) {
				roleRepository.save(new Roles(APP_ROLE.ROLE_ADMIN));
			}
		};
	}


}
