package utils;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

@Singleton
@Startup
public class EnvLoader {
    @PostConstruct
    public void init() {
        loadEnv("C:\\Users\\danis\\Desktop\\is-template-2\\ЛАБА 3\\is-lab-2-backend\\.env");
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


