package com.courseupload.repository;

import com.courseupload.model.CourseContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourseContentRepository extends JpaRepository<CourseContent, Long> {

    // Only return files belonging to this user, newest first
    List<CourseContent> findByUploadedByUsernameOrderByUploadDateDesc(String username);

    // Filter by type AND owner
    List<CourseContent> findByFileTypeContainingIgnoreCaseAndUploadedByUsername(String fileType, String username);

    // Find by id AND owner — returns empty if the file belongs to someone else
    Optional<CourseContent> findByIdAndUploadedByUsername(Long id, String username);
}
