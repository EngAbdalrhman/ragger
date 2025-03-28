# Spring AI RAG Service

A Spring Boot service implementing Retrieval-Augmented Generation (RAG) with support for multiple AI models, document management, and token-based usage tracking.

## Features

### Core RAG Capabilities

- Document processing and chunking
- Vector embeddings for semantic search
- Support for multiple AI models (Local, OpenAI, Gemini)
- Batch processing for large documents
- Caching system for frequently accessed content

### Document Management

- Support for PDF, DOCX, TXT, and other file formats
- Document versioning
- Collection-based organization
- Document metadata tracking
- Access control and permissions

### Search Capabilities

- Meilisearch integration for full-text search
- Vector similarity search
- Combined search results ranking
- Filtered and faceted search

### Token Management

- Token tracking for local model usage
- Token purchase system
- Usage estimation
- Token transfer between users

## Setup

### Prerequisites

- Java 17 or higher
- PostgreSQL 12 or higher
- Meilisearch server
- Maven

### Database Setup

```sql
-- Create database
CREATE DATABASE springai;

-- Enable required extensions
CREATE EXTENSION vector;
CREATE EXTENSION pg_stat_statements;
```

### Configuration

Update `application.properties` with your settings:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/springai
spring.datasource.username=your-username
spring.datasource.password=your-password

# Meilisearch
meilisearch.host=http://localhost:7700
meilisearch.api-key=your-master-key

# Token Management
app.token.default-limit=1000000
app.token.words-per-token=4
```

## API Endpoints

### Document Management

```bash
# Upload document
POST /api/rag/upload
Content-Type: multipart/form-data
Parameters:
- file: Document file
- collectionId: (optional) Collection ID
- tags: (optional) Document tags
- batch: (optional) Use batch processing

# Query document
POST /api/rag/query
Parameters:
- query: Search query
- modelName: (optional) AI model to use
- version: (optional) Document version
```

### Collections

```bash
# Create collection
POST /api/rag/collections
Content-Type: application/json
{
    "name": "Collection Name",
    "description": "Collection Description"
}

# List collections
GET /api/rag/collections

# Get collection documents
GET /api/rag/collections/{collectionId}/documents
```

### Token Management

```bash
# Get token balance
GET /api/tokens/balance

# Purchase tokens
POST /api/tokens/purchase
Parameters:
- amount: Number of tokens to purchase

# Estimate token usage
GET /api/tokens/estimate
Parameters:
- text: Text to estimate tokens for

# Transfer tokens
POST /api/tokens/transfer
Parameters:
- toUserId: Recipient user ID
- amount: Number of tokens to transfer
```

### Search

```bash
# Search documents
GET /api/rag/search
Parameters:
- query: Search query
- filters: (optional) Search filters
- limit: (optional) Result limit

# Similar documents
GET /api/rag/similar
Parameters:
- documentId: Reference document ID
- limit: (optional) Result limit
```

## Integration Guide

### Main Backend Integration

1. Add the service as a dependency:

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>spring-ai</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

2. Configure the service in your application:

```java
@Configuration
public class RagConfig {
    @Bean
    public RagService ragService() {
        return new RagService();
    }
}
```

3. Use the service in your controllers:

```java
@RestController
@RequestMapping("/api")
public class YourController {
    private final RagService ragService;

    public YourController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/process")
    public ResponseEntity<?> processDocument(@RequestParam("file") MultipartFile file) {
        String documentId = ragService.processAndStoreDocument(file);
        return ResponseEntity.ok(Map.of("documentId", documentId));
    }
}
```

## Best Practices

1. Document Processing

- Keep individual documents under 50MB
- Use batch processing for large documents
- Properly tag and categorize documents

2. Token Management

- Monitor token usage regularly
- Set up alerts for low token balance
- Purchase tokens in bulk for better rates

3. Search Optimization

- Use specific queries for better results
- Leverage filters for refined search
- Cache frequently accessed results

4. Performance

- Configure appropriate chunk sizes
- Monitor and adjust cache settings
- Use batch processing when appropriate

## Monitoring and Maintenance

1. Monitor token usage:

```sql
SELECT user_id, SUM(tokens_used)
FROM token_usage_logs
GROUP BY user_id;
```

2. Check search performance:

```sql
SELECT AVG(response_time)
FROM search_logs
WHERE timestamp > NOW() - INTERVAL '1 hour';
```

3. Monitor document processing:

```sql
SELECT status, COUNT(*)
FROM document_processing_logs
GROUP BY status;
```

## Security Considerations

1. API Authentication

- Use secure headers for user identification
- Implement rate limiting
- Validate all input parameters

2. Document Access

- Implement proper access controls
- Audit document access
- Encrypt sensitive content

3. Token Management

- Secure token transfer operations
- Monitor for unusual token usage
- Implement transaction logging

## Troubleshooting

Common issues and solutions:

1. Token depletion

   - Check token balance
   - Review token usage logs
   - Contact support for emergency token allocation

2. Search performance

   - Verify Meilisearch configuration
   - Check index optimization
   - Review search patterns

3. Document processing failures
   - Check file format support
   - Verify file size limits
   - Review processing logs
