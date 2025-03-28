-- Enable vector extension if not already enabled
CREATE EXTENSION IF NOT EXISTS vector;

-- Create custom domain for embedding vectors if it doesn't exist
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'embedding_vector') THEN
        CREATE DOMAIN embedding_vector AS vector(384);  -- MiniLM-L6-v2 produces 384-dimensional vectors
    END IF;
END $$;

-- Create or replace function to calculate vector similarity
CREATE OR REPLACE FUNCTION vector_similarity(a vector, b vector) 
RETURNS float
LANGUAGE plpgsql
AS $$
BEGIN
    -- Validate vector dimensions
    IF array_length(a::float[], 1) != array_length(b::float[], 1) THEN
        RAISE EXCEPTION 'Vector dimensions do not match: % != %', 
            array_length(a::float[], 1), array_length(b::float[], 1);
    END IF;
    -- Cosine similarity
    RETURN 1 - (a <-> b);
END;
$$;

-- Create index for vector similarity search if it doesn't exist
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 
        FROM pg_class c
        JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE c.relname = 'text_chunks_embedding_idx'
    ) THEN
        CREATE INDEX text_chunks_embedding_idx ON text_chunks USING ivfflat (embedding vector_cosine_ops)
        WITH (lists = 100);
    END IF;
END $$;

-- Add monitoring function for vector operations
CREATE OR REPLACE FUNCTION log_vector_operation()
RETURNS trigger AS $$
BEGIN
    INSERT INTO vector_operation_logs (
        operation_type,
        table_name,
        vector_dimension,
        operation_timestamp
    ) VALUES (
        TG_OP,
        TG_TABLE_NAME,
        array_length(NEW.embedding::float[], 1),
        current_timestamp
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create vector operation logs table if it doesn't exist
CREATE TABLE IF NOT EXISTS vector_operation_logs (
    id SERIAL PRIMARY KEY,
    operation_type TEXT,
    table_name TEXT,
    vector_dimension INTEGER,
    operation_timestamp TIMESTAMP
);

-- Create trigger for monitoring vector operations
DROP TRIGGER IF EXISTS vector_operation_trigger ON text_chunks;
CREATE TRIGGER vector_operation_trigger
    AFTER INSERT OR UPDATE
    ON text_chunks
    FOR EACH ROW
    EXECUTE FUNCTION log_vector_operation();