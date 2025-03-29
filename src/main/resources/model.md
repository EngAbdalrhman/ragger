# AI Model Integration Guide

## Core Features

- **Multi-provider support**: Local, REST, and custom model integrations
- **Message queuing**: RabbitMQ-based processing for local models
- **Token calculation**: Automatic token counting for all requests
- **License management**: API key validation and model limits

## Configuration

### 1. Database Setup

Configure models in `ai_model_config` table:

```sql
-- Example: Local model with queue
INSERT INTO ai_model_config
(model_name, provider_type, use_queue, max_tokens, is_active)
VALUES
('local-queue', 'local', true, 4096, true);

-- Example: REST model
INSERT INTO ai_model_config
(model_name, provider_type, api_url, api_key, max_tokens, is_active)
VALUES
('deepseek', 'rest', 'https://api.deepseek.ai/v1', 'YOUR_KEY', 4096, true);
```

### 2. Environment Variables

Configure in `application.properties`:

```properties
# RabbitMQ
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672

# Default model
ai.model.default-model=local-queue
```

## Usage Examples

### Basic Request

```bash
curl -X POST http://localhost:8080/api/v2/conversation/text \
  -H "Content-Type: text/plain" \
  -d "Explain quantum computing"
```

### Specifying Model

```bash
curl -X POST http://localhost:8080/api/v2/conversation/text \
  -H "Content-Type: text/plain" \
  -H "X-Model: deepseek" \
  -d "Explain quantum computing"
```

### Admin Endpoints

```bash
# List available models
curl -H "X-License-Key: YOUR_KEY" http://localhost:8080/api/admin/models

# Check token count
curl "http://localhost:8080/api/admin/tokens?text=Hello+world"
```

## Custom Model Integration

### 1. Implement ModelProvider

```java
@Component
public class CustomModelProvider implements ModelProvider {
    private final RestTemplate restTemplate;

    public String generateContent(String prompt) {
        // Custom implementation
    }
    // ... other methods
}
```

### 2. Register in ModelFactory

```java
@PostConstruct
public void init() {
    providers.put("custom", customModelProvider);
}
```

## Token Calculation

- Estimates: ~4 characters = 1 token
- Adjustable via `ModelConfig.maxTokens`

## License Enforcement

- Required header: `X-License-Key`
- Configured in `ai_license` table
- Limits active models per license

## Monitoring

Access endpoints:

- RabbitMQ: `http://localhost:15672`
- H2 Console: `http://localhost:8080/h2-console`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
