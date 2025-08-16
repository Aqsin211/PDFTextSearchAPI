package az.company.app.dao.repository;

import az.company.app.dao.entity.DocumentEntity;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DocumentRepository extends ElasticsearchRepository<DocumentEntity, UUID> {
}