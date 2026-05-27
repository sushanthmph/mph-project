package com.mphasis.tfp;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableBatchProcessing
public class TradefileprocessingandmanagementserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TradefileprocessingandmanagementserviceApplication.class, args);

    }
}
