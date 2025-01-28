package utils;

import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class EnvLoader {
    @PostConstruct
    public void init() {
        loadEnv(".env");
        System.out.println("=============== Env loaded ===============");
    }

    public static void loadEnv(String filePath) {
        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
            stream.filter(line -> line.contains("="))
                    .forEach(line -> {
                        String[] parts = line.split("=", 2);
                        // вероятно, нужно проверять существование property
                        System.setProperty(parts[0], parts[1]);
                    });
        } catch (IOException e) {
            throw new RuntimeException("Failed to load .env file", e);
        }
    }
}


