package com.motorbesitzen.gamblebot.util;

/**
 * Utility class to access environment variables.
 */
public final class EnvironmentUtil {

	/**
	 * Get the value of an environment variable.
	 *
	 * @param name The name of the environment variable to request the value of.
	 * @return The name of the environment variable with the given name.
	 */
	public static String getEnvironmentVariable(String name) {
		return System.getenv(name);
	}

}
