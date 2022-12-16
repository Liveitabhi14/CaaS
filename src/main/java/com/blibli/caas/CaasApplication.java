package com.blibli.caas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.blibli"})
public class CaasApplication {

	public static void main(String[] args) {
		SpringApplication.run(CaasApplication.class, args);
	}

}
