package az.company.app.service;

import az.company.app.dao.entity.DocumentEntity;
import az.company.app.model.response.SearchResponse;
import az.company.app.model.response.SearchResult;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.Highlight;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SearchService {

    private final ElasticsearchClient elasticsearchClient;

    public SearchService(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    public SearchResponse searchByKeyword(DocumentEntity document, String keyword, int page, int size) {
        try {
            // Validation
            if (document == null || document.getId() == null) {
                throw new IllegalArgumentException("Document and document ID must not be null");
            }
            if (!StringUtils.hasText(keyword)) {
                throw new IllegalArgumentException("Search keyword must not be null or empty");
            }
            if (page < 0) {
                throw new IllegalArgumentException("Page number must be >= 0");
            }
            if (size <= 0) {
                throw new IllegalArgumentException("Page size must be > 0");
            }

            // Build query: match content + filter by ID
            Query contentQuery = MatchQuery.of(m -> m
                    .field("content")
                    .query(keyword)
            )._toQuery();

            Query idFilter = Query.of(q -> q
                    .ids(i -> i.values(document.getId().toString()))
            );

            Query combinedQuery = Query.of(q -> q
                    .bool(b -> b
                            .must(contentQuery)
                            .filter(idFilter)
                    )
            );


            Highlight highlight = Highlight.of(h -> h
                    .fields(Map.of(
                            "content", HighlightField.of(f -> f
                                    .numberOfFragments(3)
                                    .fragmentSize(200)
                            )
                    ))
            );

            SearchRequest searchRequest = new SearchRequest.Builder()
                    .index("pdf_documents")
                    .query(combinedQuery)
                    .from(page * size)
                    .size(size)
                    .highlight(highlight)
                    .build();

            co.elastic.clients.elasticsearch.core.SearchResponse<DocumentEntity> response =
                    elasticsearchClient.search(searchRequest, DocumentEntity.class);

            // Process results
            List<SearchResult> results = new ArrayList<>();
            for (Hit<DocumentEntity> hit : response.hits().hits()) {
                List<String> fragments = hit.highlight() != null ? hit.highlight().get("content") : null;
                if (fragments != null) {
                    fragments.forEach(fragment -> results.add(new SearchResult(fragment)));
                } else {
                    results.add(new SearchResult(hit.source().getContent())); // fallback
                }
            }

            long totalHits = response.hits().total() != null ? response.hits().total().value() : 0;
            int totalPages = (int) Math.ceil((double) totalHits / size);

            return new SearchResponse(results, totalHits, totalPages);

        } catch (Exception e) {
            throw new RuntimeException("Search operation failed: " + e.getMessage(), e);
        }
    }
}
