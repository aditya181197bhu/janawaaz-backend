package com.stavya.janawaaz.repository;

import com.stavya.janawaaz.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Integer> {
}
