package az.company.app.service.abstraction;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface MinIOStorageService {
    void uploadFile(MultipartFile file, UUID uuid);

    MultipartFile downloadFile(String objectName);

    void deleteFile(String objectName);

    void ensureBucketExists(String bucket);
}
