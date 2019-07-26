package com.jethro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.jethro"})
public class VoiceRecognitionApplication {

	public static void main(String[] args) {
		SpringApplication.run(VoiceRecognitionApplication.class, args);
	}

}
