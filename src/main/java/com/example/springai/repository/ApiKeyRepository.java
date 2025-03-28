package com.example.springai.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.springai.entity.ApiKey;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    Optional<ApiKey> findByProviderAndIsActiveTrue(String provider);

    boolean existsByProviderAndIsActiveTrue(String provider);
}
