# PDFTextSearch

**PDFTextSearch** is a production-ready Spring Boot backend service designed for **full-text search on PDF documents**. It demonstrates real-world backend engineering skills, including asynchronous processing, file storage, text extraction, and search engine integration.

This project showcases modern backend best practices suitable for enterprise-level applications.

---

## Project Overview

PDFTextSearch allows users to:

* Upload PDF documents.
* Extract text content automatically using **Apache Tika**.
* Store files in **MinIO object storage**.
* Index extracted content into **Elasticsearch** for fast full-text search.
* Search PDFs by keywords and retrieve highlighted matches.
* Download or delete documents with consistent, reliable handling.

This system handles large files efficiently, uses asynchronous processing to avoid blocking operations, and provides robust error handling for reliable production usage.

---

## Key Features

* **PDF Upload & Storage**: Secure file storage in MinIO with proper naming conventions and bucket management.
* **Text Extraction**: Uses Apache Tika to extract high-quality text from PDFs.
* **Elasticsearch Integration**: Index and query PDF contents efficiently for full-text search.
* **Search API**: Highlights search results with paginated responses.
* **Asynchronous Processing**: Non-blocking text extraction and indexing for large file support.
* **Exception Handling & Logging**: Centralized, consistent error responses and structured logs.
* **Validation**: Ensures only PDFs are processed and uploaded.

---

## Technical Highlights

* **Spring Boot 3.5**: RESTful API design and microservice-ready architecture.
* **Java 21**: Modern language features including records for DTOs and concise code.
* **Apache Tika**: Reliable text extraction from PDF files.
* **Elasticsearch 8**: Powerful full-text search engine.
* **MinIO**: Scalable object storage for PDF files.
* **Asynchronous Services**: Background processing with `@Async` for performance.
* **DTOs & Immutable Records**: Clean API design using Java records for responses.
* **Logging & Monitoring**: Structured logs for debugging and operational insight.

---

## Architecture

```
Client
  │
  ▼
FileUploadController
  │
  ├─ FileServiceImpl ──> Elasticsearch (Document Indexing & Search)
  │
  └─ MinIOStorageServiceImpl ──> MinIO (PDF Storage)
```

* **Controller Layer**: REST API endpoints for file upload, download, delete, and search.
* **Service Layer**: Business logic, asynchronous processing, and file handling.
* **Repository Layer**: Elasticsearch interactions for storing and querying documents.
* **Exception Layer**: Custom exceptions and global error handling for reliability.

---

## API Endpoints

| Method | Endpoint                                         | Description                                     |
| ------ | ------------------------------------------------ | ----------------------------------------------- |
| POST   | `/file/upload`                                   | Upload a PDF                                    |
| GET    | `/file/{id}/download`                            | Download a stored PDF                           |
| DELETE | `/file/{id}`                                     | Delete a PDF and its index                      |
| GET    | `/file/{id}/search?query=keyword&page=0&size=10` | Search PDF by keyword with highlighted snippets |

---

## Project Structure & Best Practices

* **Separation of Concerns**: Controller, service, repository, and exception layers are modular and maintainable.
* **Async Processing**: Extracts and indexes PDFs in background threads for scalable performance.
* **Exception Handling**: Global error handler with structured JSON responses.
* **Validation**: File type and size validation before processing.
* **Logging**: Provides insights into operations and errors.
* **Configuration Management**: Flexible via `application.yaml` and Gradle properties.
* **DTOs & Records**: Immutable, concise response models for cleaner API design.

---

## Setup & Running

You can start **Elasticsearch**, **MinIO**, and **Kibana** (for UI) using these three commands:

```bash
# Start Elasticsearch
docker run -d --name elasticsearch -p 9200:9200 -e "discovery.type=single-node" docker.elastic.co/elasticsearch/elasticsearch:8.10.1

# Start Kibana
docker run -d --name kibana -p 5601:5601 --link elasticsearch:elasticsearch docker.elastic.co/kibana/kibana:8.10.1

# Start MinIO
docker run -d --name minio -p 9000:9000 -e MINIO_ROOT_USER=minioadmin -e MINIO_ROOT_PASSWORD=minioadmin123 minio/minio server /data
```

1. **Update Configuration**: Edit `application.yaml` to set the correct Elasticsearch and MinIO credentials if needed.

2. **Run the Service**:

```bash
./gradlew bootRun
```

3. **Access the API** at:

```
http://localhost:8080
```

---

## Example Usage

**Upload PDF**

```bash
curl -X POST "http://localhost:8080/file/upload" -F "file=@document.pdf"
```

**Search PDF**

```bash
curl "http://localhost:8080/file/{id}/search?query=keyword&page=0&size=10"
```

**Download PDF**

```bash
curl -O "http://localhost:8080/file/{id}/download"
```

**Delete PDF**

```bash
curl -X DELETE "http://localhost:8080/file/{id}"
```

---

## About

* Demonstrates **full-stack backend skills**: file storage, processing, and search engine integration.
* Follows **best practices**: async processing, exception handling, logging, modular architecture.
* Uses **modern Java features** (Java 21, records, Lombok) and **Spring Boot 3.x standards**.
* Scalable, maintainable, and production-ready architecture.
* Clear API design with documentation-ready endpoints.
