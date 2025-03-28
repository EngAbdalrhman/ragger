package com.example.springai.rag;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Entity
@Table(name = "text_chunks")
@NoArgsConstructor
@AllArgsConstructor
public class TextChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "embedding", columnDefinition = "float[]")
    private float[] embedding;

    @Column(name = "document_id")
    private String documentId;

    public TextChunk(String content) {
        this.content = content;
    }
}
