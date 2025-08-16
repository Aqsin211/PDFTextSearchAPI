package az.company.app.controller;

import az.company.app.exception.FileStorageException;
import az.company.app.model.response.FileInfo;
import az.company.app.model.response.SearchResponse;
import az.company.app.service.concrete.FileServiceImpl;
import az.company.app.service.concrete.MinIOStorageServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

import static az.company.app.model.enums.ResponseMessages.FILE_DELETED;
import static az.company.app.model.enums.ResponseMessages.INVALID_TYPE;
import static java.lang.String.format;

@RestController
@RequestMapping("/file")
public class FileUploadController {

    private final FileServiceImpl fileService;
    private final MinIOStorageServiceImpl minioService;

    public FileUploadController(FileServiceImpl fileService, MinIOStorageServiceImpl minioService) {
        this.fileService = fileService;
        this.minioService = minioService;
    }

    @PostMapping("/upload")
    public ResponseEntity<FileInfo> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty() || !file.getOriginalFilename().endsWith(".pdf")
                || !"application/pdf".equals(file.getContentType())) {
            throw new FileStorageException(INVALID_TYPE.getMessage());
        }

        UUID documentId = fileService.processFileAsync(file);
        minioService.uploadFile(file, documentId);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new FileInfo(documentId, file.getOriginalFilename()));
    }

    @GetMapping("/{id}/search")
    public ResponseEntity<SearchResponse> search(
            @PathVariable UUID id,
            @RequestParam("query") String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        SearchResponse searchResponse = fileService.searchForAppearances(id, keyword, page, size);
        return ResponseEntity.ok(searchResponse);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable UUID id) {
        String originalFileName = fileService.getFilenameById(id);
        String baseName = originalFileName.replaceFirst("[.][^.]+$", "");
        String objectName = baseName + "-" + id + ".pdf";

        MultipartFile file = minioService.downloadFile(objectName);

        try {
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + file.getOriginalFilename() + "\"")
                    .header("Content-Type", file.getContentType())
                    .body(file.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable UUID id) {
        String originalFileName = fileService.getFilenameById(id);
        String baseName = originalFileName.replaceFirst("[.][^.]+$", "");
        String objectName = baseName + "-" + id + ".pdf";

        minioService.deleteFile(objectName);
        fileService.deleteDocument(id);

        return ResponseEntity.ok(format(FILE_DELETED.getMessage(), id));
    }
}
