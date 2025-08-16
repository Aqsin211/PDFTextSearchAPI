package az.company.app.service;

import az.company.app.dao.entity.DocumentEntity;
import az.company.app.dao.repository.DocumentRepository;
import az.company.app.model.response.SearchResponse;
import az.company.app.model.response.SearchResult;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class FileService {

    private final DocumentRepository documentRepository;
    private final SearchService searchService;
    private final ElasticsearchOperations elasticsearchOperations;

    public FileService(DocumentRepository documentRepository,
                       SearchService searchService,
                       ElasticsearchOperations elasticsearchOperations) {
        this.documentRepository = documentRepository;
        this.searchService = searchService;
        this.elasticsearchOperations = elasticsearchOperations;
    }

    @PostConstruct
    public void initializeIndex() {
        try {
            IndexOperations indexOps = elasticsearchOperations.indexOps(DocumentEntity.class);
            if (!indexOps.exists()) {
                indexOps.create();
                indexOps.putMapping(DocumentEntity.class);
                log.info("Created Elasticsearch index and mapping for DocumentEntity");
            }
        } catch (Exception e) {
            log.error("Failed to initialize Elasticsearch index", e);
            throw new IllegalStateException("Failed to initialize Elasticsearch index", e);
        }
    }

    public UUID processFileAsync(MultipartFile file) {
        UUID documentId = UUID.randomUUID();
        asyncExtractAndIndex(file, documentId);
        return documentId;
    }

    @Async
    public void asyncExtractAndIndex(MultipartFile file, UUID documentId) {
        try {
            Tika tika = new Tika();
            String content = tika.parseToString(file.getInputStream());

            DocumentEntity document = DocumentEntity.builder()
                    .id(documentId)
                    .filename(file.getOriginalFilename())
                    .content(content)
                    .uploadedAt(OffsetDateTime.now())
                    .build();

            // Ensure index exists before saving
            if (!elasticsearchOperations.indexOps(DocumentEntity.class).exists()) {
                initializeIndex();
            }

            documentRepository.save(document);
            elasticsearchOperations.indexOps(DocumentEntity.class).refresh();

            log.info("Document processed and indexed successfully: {}", documentId);
        } catch (IOException | TikaException e) {
            log.error("Failed to process and index document: {}", documentId, e);
            throw new RuntimeException("Failed to process and index document", e);
        }
    }

    public SearchResponse searchForAppearances(UUID documentId, String keyword, int page, int size) {
        DocumentEntity document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
        return searchService.searchByKeyword(document, keyword, page, size);
    }


    public String getFilenameById(UUID id) {
        try {
            DocumentEntity document = documentRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Document not found: " + id));
            return document.getFilename();
        } catch (Exception e) {
            log.error("Failed to get filename for document {}", id, e);
            throw new RuntimeException("Failed to get filename", e);
        }
    }

    public void deleteDocument(UUID id) {
        try {
            DocumentEntity document = documentRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Document not found: " + id));

            documentRepository.delete(document);
            elasticsearchOperations.indexOps(DocumentEntity.class).refresh();

            log.info("Document deleted successfully: {}", id);
        } catch (Exception e) {
            log.error("Failed to delete document {}", id, e);
            throw new RuntimeException("Failed to delete document", e);
        }
    }
}