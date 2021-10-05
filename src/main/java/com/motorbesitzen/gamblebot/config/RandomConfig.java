package com.motorbesitzen.gamblebot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;
import java.util.Random;

@Configuration
public class RandomConfig {

	@Bean
	public Random buildRandom() {
		return new SecureRandom();
	}
}
