package com.motorbesitzen.gamblebot.config;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;

@Configuration
public class EventWaiterConfig {

	@Bean
	public EventWaiter buildEventWaiter() {
		// let spring control the EvenWaiter as it is recommended to only use 1 instance
		return new EventWaiter(Executors.newSingleThreadScheduledExecutor(), false);
	}
}
