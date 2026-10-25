package com.atharv.resumefit.analysis;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    Page<Analysis> findByUserId(Long userId, Pageable pageable);

    Optional<Analysis> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    List<Analysis> findTop50ByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("select avg(a.fitScore) from Analysis a where a.user.id = :userId")
    Double averageScore(@Param("userId") Long userId);
}
