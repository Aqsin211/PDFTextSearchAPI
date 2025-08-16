package az.company.app.service.abstraction;

import az.company.app.model.response.SearchResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

public interface FileService {
    @PostConstruct
    void initializeIndex();

    UUID processFileAsync(MultipartFile file);

    SearchResponse searchForAppearances(UUID documentId, String keyword, int page, int size);

    String getFilenameById(UUID documentId);

    void deleteDocument(UUID documentId);

    @Async
    void asyncExtractAndIndex(MultipartFile file, UUID documentId);
}
