-- Default model configurations
INSERT INTO ai_model_config (model_name, provider_type, api_url, api_key, max_tokens, is_active, use_queue)
VALUES 
('local', 'local', NULL, NULL, 4096, true, true),
('deepseek', 'rest', 'https://api.deepseek.ai/v1/chat/completions', '${DEEPSEEK_API_KEY}', 4096, true, false),
('openai', 'rest', 'https://api.openai.com/v1/chat/completions', '${OPENAI_API_KEY}', 4096, true, false),
('anthropic', 'rest', 'https://api.anthropic.com/v1/complete', '${ANTHROPIC_API_KEY}', 4096, true, false);

-- License table setup
CREATE TABLE IF NOT EXISTS ai_license (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    license_key VARCHAR(255) NOT NULL UNIQUE,
    valid_until TIMESTAMP NOT NULL,
    max_models INT DEFAULT 3,
    is_active BOOLEAN DEFAULT true
);

-- Sample license (for development only)
INSERT INTO ai_license (license_key, valid_until, max_models, is_active)
VALUES ('DEV-1234-5678', '2025-12-31 23:59:59', 5, true);