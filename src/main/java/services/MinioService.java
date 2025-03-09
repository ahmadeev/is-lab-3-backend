package services;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.CopyObjectArgs;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;

import java.io.InputStream;

@Getter
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

    public void uploadFile(String objectName, InputStream inputStream, long size, String contentType) throws Exception {
        System.out.println("[MinIO] file upload started");
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket("import-files")
                        .object(objectName)
                        .stream(inputStream, size, -1)
                        .contentType(contentType)
                        .build()
        );
        System.out.println("[MinIO] file upload ended");
    }

    public void deleteFile(String objectName) throws Exception {
        System.out.println("[MinIO] started file delete");
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket("import-files")
                        .object(objectName)
                        .build()
        );
        System.out.println("[MinIO] ended file delete");
    }

    // копирование временного с именем постоянного, затем удаление временного
    public void commitFile(String tempObjectName, String permanentObjectName) throws Exception {
        System.out.println("[MinIO] started file commit");
        // копирование временного
        minioClient.copyObject(
                CopyObjectArgs.builder()
                        .bucket("import-files")
                        .object(permanentObjectName)
                        .source(io.minio.CopySource.builder()
                                .bucket("import-files")
                                .object(tempObjectName)
                                .build())
                        .build()
        );
        // удаление временного
        deleteFile(tempObjectName);
        System.out.println("[MinIO] ended file commit");
    }
}
