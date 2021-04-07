package com.motorbesitzen.gamblebot.config;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventWaiterConfig {

	@Bean
	public EventWaiter buildEventWaiter() {
		// let spring control the EvenWaiter as it is recommended to only use 1 instance
		return new EventWaiter();
	}
}
