package az.company.app.service.concrete;

import az.company.app.exception.FileStorageException;
import az.company.app.service.abstraction.MinIOStorageService;
import io.minio.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.UUID;

import static az.company.app.model.enums.ResponseMessages.BUCKET_FAIL;
import static az.company.app.model.enums.ResponseMessages.FAILED_DOWNLOAD_MINIO;
import static az.company.app.model.enums.ResponseMessages.FAILED_TO_DELETE_MINIO;
import static az.company.app.model.enums.ResponseMessages.FAILED_TO_UPLOAD;
import static az.company.app.model.enums.ResponseMessages.FILE_NOT_FOUND_MINIO;
import static az.company.app.model.enums.ResponseMessages.FILE_NOT_SUPPORTED;
import static java.lang.String.format;

@Service
@Slf4j
public class MinIOStorageServiceImpl implements MinIOStorageService {

    private final MinioClient minioClient;
    private final String bucketName = "pdf-files";

    public MinIOStorageServiceImpl(
            @Value("${minio.url}") String minioUrl,
            @Value("${minio.access-key}") String accessKey,
            @Value("${minio.secret-key}") String secretKey
    ) {
        this.minioClient = MinioClient.builder()
                .endpoint(minioUrl)
                .credentials(accessKey, secretKey)
                .build();

        ensureBucketExists(bucketName);
    }

    @Override
    public void uploadFile(MultipartFile file, UUID uuid) {
        String baseName = file.getOriginalFilename()
                .replaceFirst("[.][^.]+$", "");
        String extension = ".pdf";
        String objectName = baseName + "-" + uuid + extension;

        try (InputStream fileStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(fileStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            log.info("File [{}] uploaded to MinIO bucket [{}]", objectName, bucketName);
        } catch (Exception e) {
            log.error("Failed to upload file [{}] to MinIO", objectName, e);
            throw new FileStorageException(format(FAILED_TO_UPLOAD.getMessage(), e.getMessage()));
        }
    }

    @Override
    public MultipartFile downloadFile(String objectName) {
        try (InputStream objectStream = minioClient.getObject(
                GetObjectArgs.builder().bucket(bucketName).object(objectName).build());
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = objectStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            return new InMemoryMultipartFile(objectName, outputStream.toByteArray());
        } catch (Exception e) {
            log.error("Failed to download file [{}] from MinIO", objectName, e);
            throw new FileStorageException(format(FAILED_DOWNLOAD_MINIO.getMessage(), e.getMessage()));
        }
    }

    @Override
    public void deleteFile(String objectName) {
        try {
            boolean fileExists = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            ) != null;

            if (fileExists) {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .build()
                );
                log.info("File [{}] deleted from MinIO bucket [{}]", objectName, bucketName);
            } else {
                log.warn("File [{}] not found in MinIO bucket [{}]", objectName, bucketName);
                throw new FileStorageException(format(FILE_NOT_FOUND_MINIO.getMessage(), objectName));
            }
        } catch (Exception e) {
            log.error("Failed to delete file [{}] from MinIO", objectName, e);
            throw new FileStorageException(format(FAILED_TO_DELETE_MINIO.getMessage(), objectName));
        }
    }

    @Override
    public void ensureBucketExists(String bucket) {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build()
            );

            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucket).build()
                );
                log.info("Bucket [{}] created", bucket);
            }
        } catch (Exception e) {
            log.error("Failed to check/create MinIO bucket [{}]", bucket, e);
            throw new FileStorageException(format(BUCKET_FAIL.getMessage(), e.getMessage()));
        }
    }

    private static class InMemoryMultipartFile implements MultipartFile {

        private final String fileName;
        private final byte[] content;

        public InMemoryMultipartFile(String fileName, byte[] content) {
            this.fileName = fileName;
            this.content = content;
        }

        @Override
        public String getName() {
            return fileName;
        }

        @Override
        public String getOriginalFilename() {
            return fileName;
        }

        @Override
        public String getContentType() {
            return "application/pdf";
        }

        @Override
        public boolean isEmpty() {
            return content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() {
            return content;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(java.io.File dest) {
            throw new UnsupportedOperationException(FILE_NOT_SUPPORTED.getMessage());
        }
    }
}
