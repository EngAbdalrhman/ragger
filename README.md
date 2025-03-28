# Spring AI Model Factory

This Spring Boot application demonstrates the implementation of a Factory Pattern for different AI models using Spring AI. It provides a unified interface to interact with various AI models like OpenAI's GPT and Google's Gemini, as well as support for custom local models.

## Features

- Factory Pattern implementation for AI model selection
- Support for multiple AI models:
  - OpenAI (GPT)
  - Google Vertex AI (Gemini)
  - Custom Local Model
- RESTful API endpoints
- Error handling and logging
- Environment-based configuration

## Prerequisites

- Java 17 or higher
- Maven
- OpenAI API Key (for OpenAI integration)
- Google Cloud Project and credentials (for Vertex AI/Gemini integration)

## Configuration

Set the following environment variables:

```bash
export OPENAI_API_KEY=your_openai_api_key
export GOOGLE_CLOUD_PROJECT=your_google_cloud_project_id
```

## API Endpoints

### Execute AI Model
```http
POST /api/ai/execute
Content-Type: application/json

{
    "modelName": "openai",
    "input": "Your prompt here"
}
```

Available model names:
- `openai` - Uses OpenAI's GPT model
- `gemini` - Uses Google's Gemini model
- `local` - Uses custom local model implementation

### List Available Models
```http
GET /api/ai/models
```

## Response Format

```json
{
    "result": "AI model response",
    "modelUsed": "openai"
}
```

## Error Handling

The API returns appropriate HTTP status codes and error messages:

- 404: Model not found
- 500: Internal server error

Error response format:
```json
{
    "error": "Error type",
    "message": "Detailed error message"
}
```

## Building and Running

1. Clone the repository
2. Set required environment variables
3. Build the project:
   ```bash
   mvn clean install
   ```
4. Run the application:
   ```bash
   mvn spring-boot:run
   ```

## Example Usage

```bash
# Execute OpenAI model
curl -X POST http://localhost:8080/api/ai/execute \
  -H "Content-Type: application/json" \
  -d '{"modelName": "openai", "input": "What is the capital of France?"}'

# Execute Gemini model
curl -X POST http://localhost:8080/api/ai/execute \
  -H "Content-Type: application/json" \
  -d '{"modelName": "gemini", "input": "Explain quantum computing"}'