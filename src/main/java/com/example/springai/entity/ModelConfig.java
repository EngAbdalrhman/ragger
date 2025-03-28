package com.example.springai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "model_configs")
@NoArgsConstructor
@AllArgsConstructor
public class ModelConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider", nullable = false)
    private String provider;  // e.g., "openai", "gemini", "local"

    @Column(name = "model_name", nullable = false)
    private String modelName;  // e.g., "gpt-4", "gemini-pro"

    @Column(name = "temperature")
    private Double temperature;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "additional_params")
    private String additionalParams;  // Store additional parameters as JSON
}
