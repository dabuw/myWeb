package com.portfolio.portfolio.dto;

import com.portfolio.portfolio.model.Profile;
import com.portfolio.portfolio.model.ProjectLink;

import java.util.List;

public record PortfolioResponse(Profile profile, List<ProjectLink> projects) {
}
