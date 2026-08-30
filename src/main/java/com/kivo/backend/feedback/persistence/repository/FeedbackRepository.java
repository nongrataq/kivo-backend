package com.kivo.backend.feedback.persistence.repository;

import com.kivo.backend.feedback.persistence.entity.Feedback;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {}
