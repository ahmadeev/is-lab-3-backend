package services;

import io.minio.MinioClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MinioService {
    private final MinioClient minioClient;

    @Inject
    public MinioService() {
        String minioUrl = System.getProperty("MINIO_URL", System.getenv("MINIO_URL"));
        String minioUser = System.getProperty("MINIO_USER", System.getenv("MINIO_USER"));
        String minioPassword = System.getProperty("MINIO_PASSWORD", System.getenv("MINIO_PASSWORD"));

        if (minioUrl == null || minioUser == null || minioPassword == null) {
            throw new IllegalStateException("MinIO configuration is missing. Please check your environment variables.");
        }

        this.minioClient = MinioClient.builder()
                .endpoint(minioUrl)
                .credentials(minioUser, minioPassword)
                .build();
    }

    public MinioClient getClient() {
        return minioClient;
    }
}
