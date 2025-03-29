# AI Service Testing Instructions

## 1. Basic Configuration Check

```bash
curl -X GET http://localhost:8080/api/ai-test/config
```

Expected Response:

```json
{
  "status": "active",
  "service": "AI Test Endpoint",
  "message": "Configuration verified"
}
```

## 2. Full Integration Test

```bash
curl -X POST http://localhost:8080/api/ai-test/analyze \
  -H "Content-Type: text/plain" \
  -d "I want to create a customer management app"
```

Expected Successful Response:

```json
{
  "analysis": {
    "operation": "create",
    "message": "I'll help create a customer management app",
    "entities": ["customer", "management", "app"]
  },
  "messageStatus": "queued"
}
```

## 3. Error Case Test

```bash
curl -X POST http://localhost:8080/api/ai-test/analyze \
  -H "Content-Type: text/plain" \
  -d ""
```

Expected Error Response:

```json
{
  "error": "Prompt cannot be empty"
}
```

## Verification Steps

1. Check application logs for:

   - RabbitMQ connection success
   - Queue creation
   - Message processing

2. Verify RabbitMQ queue:

```bash
# Requires rabbitmqadmin
rabbitmqadmin get queue=ai.local.requests count=10
```

3. Check dead letter queue if needed:

```bash
rabbitmqadmin get queue=ai.local.requests.dlq
```
