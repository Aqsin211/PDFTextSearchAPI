package az.company.app.controller;

import az.company.app.model.response.FileInfo;
import az.company.app.model.response.SearchResponse;
import az.company.app.service.FileService;
import az.company.app.service.MinIOStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/file")
public class FileUploadController {

    private final FileService fileService;
    private final MinIOStorageService minioService;

    public FileUploadController(FileService fileService, MinIOStorageService minioService) {
        this.fileService = fileService;
        this.minioService = minioService;
    }

    @PostMapping("/upload")
    public ResponseEntity<FileInfo> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty() || !file.getOriginalFilename().endsWith(".pdf")
                || !"application/pdf".equals(file.getContentType())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new FileInfo(null, "Invalid file type. Only PDFs are allowed."));
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
        return ResponseEntity.ok(fileService.searchForAppearances(id, keyword, page, size));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable UUID id) throws IOException {
        String storedFileName = fileService.getFilenameById(id);
        String baseName = storedFileName.replaceFirst("[.][^.]+$", "");
        String objectName = baseName + "-" + id + ".pdf";
        MultipartFile file = minioService.downloadFile(objectName);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + file.getOriginalFilename() + "\"")
                .header("Content-Type", file.getContentType())
                .body(file.getBytes());
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable UUID id) {
        String storedFileName = fileService.getFilenameById(id);
        String baseName = storedFileName.replaceFirst("[.][^.]+$", "");
        String objectName = baseName + "-" + id + ".pdf";
        minioService.deleteFile(objectName);
        fileService.deleteDocument(id);
        return ResponseEntity.ok("Deleted file with id: " + id);
    }
}
