package com.example.salonflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@SpringBootApplication
public class SalonflowApplication {

	public static void main(String[] args) {
		loadEnv();
		SpringApplication.run(SalonflowApplication.class, args);
	}

	private static void loadEnv() {
		try {
			if (Files.exists(Paths.get(".env"))) {
				List<String> lines = Files.readAllLines(Paths.get(".env"));
				for (String line : lines) {
					line = line.trim();
					if (line.isEmpty() || line.startsWith("#")) {
						continue;
					}
					int separator = line.indexOf('=');
					if (separator > 0) {
						String key = line.substring(0, separator).trim();
						String value = line.substring(separator + 1).trim();
						if (value.startsWith("\"") && value.endsWith("\"")) {
							value = value.substring(1, value.length() - 1);
						} else if (value.startsWith("'") && value.endsWith("'")) {
							value = value.substring(1, value.length() - 1);
						}
						System.setProperty(key, value);
					}
				}
			}
		} catch (IOException e) {
			System.err.println("Could not load .env file: " + e.getMessage());
		}
	}

}
