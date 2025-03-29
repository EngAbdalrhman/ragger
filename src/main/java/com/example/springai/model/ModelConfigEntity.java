package com.example.springai.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "ai_model_config")
@Data
public class ModelConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String modelName;

    @Column(nullable = false)
    private String providerType; // local, rest, etc.

    @Column
    private String apiUrl;

    @Column
    private String apiKey;

    @Column
    private Integer maxTokens = 4096;

    @Column
    private Boolean isActive = true;

    @Column
    private Boolean useQueue = false;
}
