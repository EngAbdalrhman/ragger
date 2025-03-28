-- Enable vector extension if not already enabled
CREATE EXTENSION IF NOT EXISTS vector;

-- Create or replace function to calculate vector similarity
CREATE OR REPLACE FUNCTION vector_similarity(a vector, b vector) 
RETURNS float
LANGUAGE plpgsql
AS $$
BEGIN
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