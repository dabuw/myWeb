package com.portfolio.portfolio.service;

import com.portfolio.portfolio.dto.PortfolioResponse;
import com.portfolio.portfolio.model.Profile;
import com.portfolio.portfolio.model.ProjectLink;
import com.portfolio.portfolio.repository.ProfileRepository;
import com.portfolio.portfolio.repository.ProjectLinkRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class PortfolioService {

    private static final long SINGLE_PROFILE_ID = 1L;

    private final ProfileRepository profileRepository;
    private final ProjectLinkRepository projectLinkRepository;

    public PortfolioService(ProfileRepository profileRepository, ProjectLinkRepository projectLinkRepository) {
        this.profileRepository = profileRepository;
        this.projectLinkRepository = projectLinkRepository;
    }

    public PortfolioResponse getPortfolio() {
        Profile profile = profileRepository.findById(SINGLE_PROFILE_ID)
                .orElseThrow(() -> new EntityNotFoundException("Profile not initialized"));
        List<ProjectLink> links = projectLinkRepository.findAllByOrderByDisplayOrderAscIdAsc();
        return new PortfolioResponse(profile, links);
    }

    public Profile saveProfile(Profile profile) {
        profile.setId(SINGLE_PROFILE_ID);
        return profileRepository.save(profile);
    }

    public ProjectLink addProject(ProjectLink projectLink) {
        projectLink.setId(null);
        return projectLinkRepository.save(projectLink);
    }

    public ProjectLink updateProject(Long id, ProjectLink projectLink) {
        Long projectId = Objects.requireNonNull(id, "Project id is required");
        ProjectLink existing = projectLinkRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + id));

        existing.setName(projectLink.getName());
        existing.setUrl(projectLink.getUrl());
        existing.setDescription(projectLink.getDescription());
        existing.setHighlight(projectLink.getHighlight());
        existing.setDisplayOrder(projectLink.getDisplayOrder());

        return projectLinkRepository.save(existing);
    }

    public void deleteProject(Long id) {
        Long projectId = Objects.requireNonNull(id, "Project id is required");
        if (!projectLinkRepository.existsById(projectId)) {
            throw new EntityNotFoundException("Project not found: " + id);
        }
        projectLinkRepository.deleteById(projectId);
    }
}
