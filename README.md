# **Spring AI Agent Service**

An intelligent Spring Boot service designed to act as an AI agent that interacts with users through natural language to understand their requirements and communicate with a main backend system.

---

## **1. Overview**

The **Spring AI Agent Service** enables backend systems to:

- Process and index documents
- Perform semantic searches using vector embeddings
- Integrate with AI models (Local, OpenAI, Gemini)
- Manage and track token usage
- Seamlessly integrate with existing backend infrastructures

### **System Architecture**

```uml
Main Backend <--> AI Agent Service <--> AI Models (Local/OpenAI/Gemini)
     |                   |
     └───────[Shared]────┘
          PostgreSQL DB
```

---

## **2. Features**

### **2.1 Core RAG Capabilities**

- Document processing and chunking
- Vector-based semantic search
- Multi-model support: Local models, OpenAI, Gemini
- Batch document processing
- Caching for optimized performance

### **2.2 Document Management**

- Support for PDF, DOCX, TXT, and other formats
- Document version control
- Collection-based organization
- Metadata tracking and tagging
- Access control and user permissions

### **2.3 Search Capabilities**

- **Meilisearch integration** for full-text search
- **Vector similarity search** using embeddings
- Hybrid ranking for enhanced relevance
- **Filtered and faceted search**

### **2.4 Token Management**

- Real-time token tracking
- Token purchase and transfer system
- Usage estimation and cost forecasting
- Secure token transactions

---

## **3. Setup Instructions**

### **3.1 Prerequisites**

Ensure the following dependencies are installed:

- **Java 17+**
- **PostgreSQL 12+**
- **Meilisearch Server**
- **Maven Build System**

### **3.2 Database Configuration**

Execute the following SQL commands:

```sql
-- Create the database
CREATE DATABASE springai;

-- Enable required extensions
CREATE EXTENSION vector;
CREATE EXTENSION pg_stat_statements;
```

### **3.3 Application Configuration**

Modify `application.properties` accordingly:

```properties
# Database Connection
spring.datasource.url=jdbc:postgresql://localhost:5432/springai
spring.datasource.username=your-username
spring.datasource.password=your-password

# Meilisearch Configuration
meilisearch.host=http://localhost:7700
meilisearch.api-key=your-master-key

# Token Management
app.token.default-limit=1000000
app.token.words-per-token=4
```

---

## **4. API Endpoints**

### **4.1 Document Management**

#### Upload Document

```http
POST /api/rag/upload
Content-Type: multipart/form-data
```

**Parameters:**

- `file`: The document file to upload
- `collectionId`: _(Optional)_ Collection ID for grouping
- `tags`: _(Optional)_ Document tags
- `batch`: _(Optional)_ Enables batch processing

#### Query Documents

```http
POST /api/rag/query
```

**Parameters:**

- `query`: Search query string
- `modelName`: _(Optional)_ AI model selection
- `version`: _(Optional)_ Specific document version

### **4.2 Collections**

#### Create Collection

```http
POST /api/rag/collections
Content-Type: application/json
```

**Request Body:**

```json
{
  "name": "Collection Name",
  "description": "Collection Description"
}
```

#### List All Collections

```http
GET /api/rag/collections
```

#### Retrieve Documents in a Collection

```http
GET /api/rag/collections/{collectionId}/documents
```

### **4.3 Token Management**

#### Get Token Balance

```http
GET /api/tokens/balance
```

#### Purchase Tokens

```http
POST /api/tokens/purchase
```

**Parameters:**

- `amount`: Number of tokens to purchase

#### Estimate Token Usage

```http
GET /api/tokens/estimate
```

**Parameters:**

- `text`: Input text for estimation

#### Transfer Tokens

```http
POST /api/tokens/transfer
```

**Parameters:**

- `toUserId`: Recipient user ID
- `amount`: Number of tokens to transfer

### **4.4 Search Operations**

#### Search for Documents

```http
GET /api/rag/search
```

**Parameters:**

- `query`: Search keyword
- `filters`: _(Optional)_ Search filters
- `limit`: _(Optional)_ Maximum results

#### Find Similar Documents

```http
GET /api/rag/similar
```

**Parameters:**

- `documentId`: Reference document ID
- `limit`: _(Optional)_ Maximum results

---

## **5. Integration Guide**

### **5.1 Dependency Installation**

Add the service to your Maven project:

```xml

<dependency>
    <groupId>com.example</groupId>
    <artifactId>spring-ai</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### **5.2 Configuration**

```java
@Configuration
public class RagConfig {
    @Bean
    public RagService ragService() {
        return new RagService();
    }
}
```

### **5.3 Controller Example**

```java
@RestController
@RequestMapping("/api")
public class DocumentController {
    private final RagService ragService;

    public DocumentController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/process")
    public ResponseEntity<?> processDocument(@RequestParam("file") MultipartFile file) {
        String documentId = ragService.processAndStoreDocument(file);
        return ResponseEntity.ok(Map.of("documentId", documentId));
    }
}
```

---

## **6. Best Practices**

### **6.1 Document Processing**

- Maintain documents under **50MB** for efficiency
- Use **batch processing** for large files
- Apply **tags and metadata** for better organization

### **6.2 Token Management**

- Regularly monitor usage via analytics
- Set alerts for **low token balance**
- Optimize model calls to reduce token consumption

### **6.3 Search Optimization**

- Use **precise queries** for best results
- Apply **filtering and faceted search** for better refinement
- Cache frequently accessed results

### **6.4 Performance Considerations**

- Optimize **chunk sizes** for vector processing
- Adjust cache configurations as needed
- Utilize **batch processing** for high workloads

---

## **7. Security Considerations**

### **7.1 API Security**

- Implement **authentication** and **rate limiting**
- Validate all incoming parameters
- Secure sensitive endpoints

### **7.2 Data Protection**

- Implement **access controls** for documents
- Encrypt sensitive content
- Audit document access logs

### **7.3 Token Security**

- Secure **token transfer operations**
- Monitor usage patterns for anomalies
- Implement logging for all transactions

---

## **8. Troubleshooting**

| Issue                           | Solution                                              |
| ------------------------------- | ----------------------------------------------------- |
| **Token depletion**             | Check balance, review logs, request additional tokens |
| **Slow search**                 | Optimize Meilisearch indexes, analyze query patterns  |
| **Document processing failure** | Check format support, verify file size constraints    |

---

### **Contact Support for Assistance** 🚀
