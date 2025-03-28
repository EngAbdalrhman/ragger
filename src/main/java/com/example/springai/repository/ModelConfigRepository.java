package com.example.springai.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.springai.entity.ModelConfig;

@Repository
public interface ModelConfigRepository extends JpaRepository<ModelConfig, Long> {

    Optional<ModelConfig> findByProviderAndIsActiveTrue(String provider);

    Optional<ModelConfig> findByIsDefaultTrue();

    List<ModelConfig> findAllByIsActiveTrue();

    boolean existsByProviderAndModelNameAndIsActiveTrue(String provider, String modelName);
}
