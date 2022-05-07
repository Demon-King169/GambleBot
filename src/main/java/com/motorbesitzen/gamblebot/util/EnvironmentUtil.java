package com.motorbesitzen.gamblebot.util;

public final class EnvironmentUtil {

	public static String getEnvironmentVariable(String name) {
		return System.getenv(name);
	}

	public static String getEnvironmentVariableOrDefault(String name, String defaultValue) {
		return System.getenv().getOrDefault(name, defaultValue);
	}
}
