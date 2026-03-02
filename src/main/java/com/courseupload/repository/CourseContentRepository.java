package com.courseupload.repository;

import com.courseupload.model.CourseContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseContentRepository extends JpaRepository<CourseContent, Long> {
    List<CourseContent> findByFileTypeContainingIgnoreCase(String fileType);
    List<CourseContent> findAllByOrderByUploadDateDesc();
}
