package az.company.app.service.abstraction;

import az.company.app.dao.entity.DocumentEntity;
import az.company.app.model.response.SearchResponse;

public interface SearchService {
    SearchResponse searchByKeyword(DocumentEntity document, String keyword, int page, int size);
}
