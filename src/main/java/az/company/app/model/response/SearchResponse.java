package az.company.app.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {
    List<SearchResult> results;
    long totalFound;
    int totalPages;
}
