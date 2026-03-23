package com.portfolio.portfolio.repository;

import com.portfolio.portfolio.model.ProjectLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectLinkRepository extends JpaRepository<ProjectLink, Long> {
    List<ProjectLink> findAllByOrderByDisplayOrderAscIdAsc();
}
