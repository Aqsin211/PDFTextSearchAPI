package az.company.app.dao.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document(indexName = "pdf_documents")
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocumentEntity {
    @Id
    @Field(type = FieldType.Keyword)
    UUID id;

    String filename;

    @Field(type = FieldType.Text)
    String content;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private OffsetDateTime uploadedAt;
}
