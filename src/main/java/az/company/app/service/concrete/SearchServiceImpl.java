package az.company.app.service.concrete;

import az.company.app.dao.entity.DocumentEntity;
import az.company.app.exception.SearchException;
import az.company.app.model.response.SearchResponse;
import az.company.app.model.response.SearchResult;
import az.company.app.service.abstraction.SearchService;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.Highlight;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static az.company.app.model.enums.ResponseMessages.DOCUMENT_CAN_NOT_BE_NULL;
import static az.company.app.model.enums.ResponseMessages.FAILED_SEARCH_FOR_KEYWORD;
import static java.lang.String.format;

@Service
public class SearchServiceImpl implements SearchService {

    private final ElasticsearchClient elasticsearchClient;

    public SearchServiceImpl(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    @Override
    public SearchResponse searchByKeyword(DocumentEntity document, String keyword, int page, int size) {
        if (document == null || document.getId() == null) {
            throw new SearchException(DOCUMENT_CAN_NOT_BE_NULL.getMessage());
        }

        try {
            // Build the search queries
            Query contentQuery = MatchQuery.of(match -> match
                    .field("content")
                    .query(keyword)
            )._toQuery();

            Query idFilter = Query.of(query -> query
                    .ids(idsQuery -> idsQuery.values(document.getId().toString()))
            );

            Query combinedQuery = Query.of(query -> query
                    .bool(boolQuery -> boolQuery
                            .must(contentQuery)
                            .filter(idFilter)
                    )
            );

            // Configure highlighting
            Highlight highlight = Highlight.of(h -> h.fields(Map.of(
                    "content", HighlightField.of(f -> f
                            .numberOfFragments(50)
                            .fragmentSize(200)
                    )
            )));

            // Build the search request
            SearchRequest searchRequest = new SearchRequest.Builder()
                    .index("pdf_documents")
                    .query(combinedQuery)
                    .size(1)
                    .highlight(highlight)
                    .build();

            var searchResponse = elasticsearchClient.search(searchRequest, DocumentEntity.class);

            // Extract highlighted snippets
            List<String> allSnippets = new ArrayList<>();
            if (!searchResponse.hits().hits().isEmpty()) {
                var firstHit = searchResponse.hits().hits().get(0);
                List<String> highlightedFragments = firstHit.highlight() != null
                        ? firstHit.highlight().get("content")
                        : null;

                if (highlightedFragments != null) {
                    allSnippets.addAll(highlightedFragments);
                }
            }

            // Pagination
            int totalSnippets = allSnippets.size();
            int totalPages = (int) Math.ceil((double) totalSnippets / size);
            int fromIndex = page * size;
            int toIndex = Math.min(fromIndex + size, totalSnippets);

            List<SearchResult> pagedResults = new ArrayList<>();
            if (fromIndex < totalSnippets) {
                for (String snippet : allSnippets.subList(fromIndex, toIndex)) {
                    pagedResults.add(new SearchResult(snippet));
                }
            }

            return new SearchResponse(pagedResults, totalSnippets, totalPages);

        } catch (Exception e) {
            throw new SearchException(format(FAILED_SEARCH_FOR_KEYWORD.getMessage(),keyword,e.getMessage()));
        }
    }
}
