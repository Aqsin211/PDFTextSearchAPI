package az.company.app.service.concrete;

import az.company.app.dao.entity.DocumentEntity;
import az.company.app.dao.repository.DocumentRepository;
import az.company.app.exception.DocumentNotFoundException;
import az.company.app.exception.DocumentProcessingException;
import az.company.app.model.response.SearchResponse;
import az.company.app.service.abstraction.FileService;
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
import java.util.UUID;

import static az.company.app.model.enums.ResponseMessages.FAILED_TO_DELETE_DOCUMENT;
import static az.company.app.model.enums.ResponseMessages.FAILED_TO_INITIALIZE;
import static java.lang.String.format;

@Slf4j
@Service
public class FileServiceImpl implements FileService {

    private final DocumentRepository documentRepository;
    private final SearchServiceImpl searchService;
    private final ElasticsearchOperations elasticsearchOperations;

    public FileServiceImpl(DocumentRepository documentRepository,
                           SearchServiceImpl searchService,
                           ElasticsearchOperations elasticsearchOperations) {
        this.documentRepository = documentRepository;
        this.searchService = searchService;
        this.elasticsearchOperations = elasticsearchOperations;
    }

    @PostConstruct
    @Override
    public void initializeIndex() {
        try {
            IndexOperations indexOperations = elasticsearchOperations.indexOps(DocumentEntity.class);

            if (!indexOperations.exists()) {
                indexOperations.create();
                indexOperations.putMapping(DocumentEntity.class);
                log.info("Created Elasticsearch index and mapping for DocumentEntity");
            }
        } catch (Exception e) {
            log.error("Failed to initialize Elasticsearch index", e);
            throw new DocumentProcessingException(format(FAILED_TO_INITIALIZE.getMessage(), e.getMessage()));
        }
    }

    @Override
    public UUID processFileAsync(MultipartFile file) {
        UUID documentId = UUID.randomUUID();
        asyncExtractAndIndex(file, documentId);
        return documentId;
    }

    @Override
    public SearchResponse searchForAppearances(UUID documentId, String keyword, int page, int size) {
        DocumentEntity document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        try {
            return searchService.searchByKeyword(document, keyword, page, size);
        } catch (Exception e) {
            log.error("Failed to search for appearances in document {}", documentId, e);
            throw new DocumentProcessingException(format(FAILED_TO_INITIALIZE.getMessage(), e.getMessage()));
        }
    }

    @Override
    public String getFilenameById(UUID documentId) {
        DocumentEntity document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
        return document.getFilename();
    }

    @Override
    public void deleteDocument(UUID documentId) {
        DocumentEntity document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
        try {
            documentRepository.delete(document);
            elasticsearchOperations.indexOps(DocumentEntity.class).refresh();
            log.info("Document deleted successfully: {}", documentId);
        } catch (Exception e) {
            log.error("Failed to delete document {}", documentId, e);
            throw new DocumentProcessingException(format(FAILED_TO_DELETE_DOCUMENT.getMessage(), e.getMessage()));
        }
    }

    @Async
    @Override
    public void asyncExtractAndIndex(MultipartFile file, UUID documentId) {
        try {
            Tika tika = new Tika();
            String extractedContent = tika.parseToString(file.getInputStream());

            DocumentEntity document = DocumentEntity.builder()
                    .id(documentId)
                    .filename(file.getOriginalFilename())
                    .content(extractedContent)
                    .uploadedAt(OffsetDateTime.now())
                    .build();

            if (!elasticsearchOperations.indexOps(DocumentEntity.class).exists()) {
                initializeIndex();
            }

            documentRepository.save(document);
            elasticsearchOperations.indexOps(DocumentEntity.class).refresh();

            log.info("Document processed and indexed successfully: {}", documentId);
        } catch (IOException | TikaException e) {
            log.error("Failed to process and index document: {}", documentId, e);
            throw new DocumentProcessingException(format(FAILED_TO_INITIALIZE.getMessage(), e.getMessage()));
        }
    }
}
