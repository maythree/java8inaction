package com.daou;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.web.reactive.config.EnableWebFlux;

@EnableWebFlux
@SpringBootApplication
public class Application {
	public static void main(String[] args) {
		SpringApplication springApplication = new SpringApplication(Application.class);
		ConfigurableApplicationContext configurableApplicationContext = springApplication.run(args);
		ConfigurableEnvironment configurableEnvironment = configurableApplicationContext.getEnvironment();

//		configurableEnvironment.getSystemEnvironment().forEach((k, v) -> {
//			System.out.println(k + ": " + (String) v);
//		});
//
//		System.out.println("==============================================");
//
//		configurableEnvironment.getSystemProperties().forEach((k, v) -> {
//			System.out.println(k + ": " + (String) v);
//		});
	}

	@Bean
	public CommandLineRunner run1() {
		return new CommandLineRunner() {
			@Override
			public void run(String... args) throws Exception {
				System.out.println("run11111...");
			}
		};
	}

	@Bean
	public ApplicationRunner run2() {
		return new ApplicationRunner() {
			@Override
			public void run(ApplicationArguments args) throws Exception {
				System.out.println("run22222...");
			}
		};
	}
}
