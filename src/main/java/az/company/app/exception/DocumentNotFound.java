package az.company.app.exception;

import java.util.UUID;

public class DocumentNotFound extends RuntimeException {
    public DocumentNotFound(UUID documentId) {
        super("Document not found: " + documentId);
    }
}