package az.company.app.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseMessages {
    FAILED_SEARCH_FOR_KEYWORD("Failed to search for keyword '%s': %s"),
    DOCUMENT_CAN_NOT_BE_NULL("Document and document ID must not be null"),
    BUCKET_FAIL("Failed to check/create MinIO bucket: %s"),
    FAILED_TO_DELETE_MINIO("Failed to delete file from MinIO: %s"),
    FILE_NOT_FOUND_MINIO("File not found in MinIO: %s"),
    FAILED_DOWNLOAD_MINIO("Failed to download file from MinIO: %s"),
    FILE_NOT_SUPPORTED("Transfer to File is not supported."),
    FAILED_TO_UPLOAD("Failed to upload file to MinIO: %s"),
    FAILED_TO_DELETE_DOCUMENT("Failed to delete document: %s"),
    FAILED_TO_INITIALIZE("Failed to initialize Elasticsearch index: %s"),
    UNEXPECTED_ERROR("An unexpected error occurred: %s"),
    FILE_DELETED("Deleted file with id: %s"),
    INVALID_TYPE("Invalid file type. Only PDFs are allowed.");
    private final String message;
}
