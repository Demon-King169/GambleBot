package com.motorbesitzen.gamblebot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;
import java.util.Random;

/**
 * Configures the generation of the {@code Random} object that gets used for all the randomness in games.
 * It generates a {@code SecureRandom} object which provides the randomness; might be over the top for
 * this use-case as there are no security details that depend on the randomness. However, {@code SecureRandom}
 * is a lot less predictable than {@code Random} (that should not be an issue, but better safe than sorry).
 */
@Configuration
public class RandomConfig {

	@Bean
	public Random buildRandom() {
		return new SecureRandom();
	}
}
