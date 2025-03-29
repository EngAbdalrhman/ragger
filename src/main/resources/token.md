# Token Management API Documentation

## Overview

The Token API provides endpoints for:

- License key validation
- Token calculation
- Token balance management
- Request authorization

## Base URL

`http://localhost:8080/api/tokens`

## Authentication

All requests require:

```http
X-License-Key: YOUR_LICENSE_KEY
```

## Endpoints

### 1. Check Remaining Tokens

```http
GET /remaining
```

**Response:**

```json
{
  "remaining": 850
}
```

### 2. Add Tokens

```http
POST /add?tokens=1000
```

**Response:**  
HTTP 204 No Content

### 3. Calculate Tokens

```http
GET /calculate?text=Your+input+text
```

**Response:**

```json
{
  "tokenCount": 3
}
```

### 4. Validate Request (Internal)

```java
// Used by other services
boolean canProcess = licenseService.canMakeRequest(licenseKey, inputText);
```

## Token Calculation Logic

- 4 characters ≈ 1 token
- Minimum 1 token per request
- Formula: `ceil(text.length() / 4.0)`

## License Management

Pre-configured demo licenses:

```java
"DEMO-KEY-1" - 1000 tokens
"DEMO-KEY-2" - 5000 tokens
```

## Integration Example

```java
// Check if request can be processed
if (licenseService.canMakeRequest(licenseKey, inputText)) {
    int tokens = tokenService.calculateTokens(inputText);
    licenseService.deductTokens(licenseKey, tokens);
    // Process request
}
```

## Error Responses

```json
{
  "error": "Invalid license key",
  "status": 403
}
```
