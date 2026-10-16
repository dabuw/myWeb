package com.portfolio.portfolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "profiles")
public class Profile {

    @Id
    private Long id;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 1600)
    private String summary;

    @Column(length = 120)
    private String location;

    @Column(length = 160)
    private String email;

    @Column(length = 255)
    private String githubUrl;

    @Column(length = 255)
    private String linkedinUrl;

    // --- English translations (nullable: the English page falls back to the
    // Chinese value when a field is left empty) ---

    @Column(length = 100)
    private String fullNameEn;

    @Column(length = 120)
    private String titleEn;

    @Column(length = 1600)
    private String summaryEn;

    @Column(length = 120)
    private String locationEn;

    // --- 展示开关：控制公开页显示哪些联系方式 ---
    // 由管理页勾选。历史数据里这些列是 NULL，前端把 NULL 视为「显示」，
    // 所以升级后内容不会突然消失；一旦在管理页保存过，就变成明确的 true/false。
    // 注意：开关为真只是「允许显示」，对应内容为空时前端依然不渲染。

    @Column(name = "show_location")
    private Boolean showLocation;

    @Column(name = "show_email")
    private Boolean showEmail;

    @Column(name = "show_github")
    private Boolean showGithub;

    @Column(name = "show_linkedin")
    private Boolean showLinkedin;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public void setGithubUrl(String githubUrl) {
        this.githubUrl = githubUrl;
    }

    public String getLinkedinUrl() {
        return linkedinUrl;
    }

    public void setLinkedinUrl(String linkedinUrl) {
        this.linkedinUrl = linkedinUrl;
    }

    public String getFullNameEn() {
        return fullNameEn;
    }

    public void setFullNameEn(String fullNameEn) {
        this.fullNameEn = fullNameEn;
    }

    public String getTitleEn() {
        return titleEn;
    }

    public void setTitleEn(String titleEn) {
        this.titleEn = titleEn;
    }

    public String getSummaryEn() {
        return summaryEn;
    }

    public void setSummaryEn(String summaryEn) {
        this.summaryEn = summaryEn;
    }

    public String getLocationEn() {
        return locationEn;
    }

    public void setLocationEn(String locationEn) {
        this.locationEn = locationEn;
    }

    public Boolean getShowLocation() {
        return showLocation;
    }

    public void setShowLocation(Boolean showLocation) {
        this.showLocation = showLocation;
    }

    public Boolean getShowEmail() {
        return showEmail;
    }

    public void setShowEmail(Boolean showEmail) {
        this.showEmail = showEmail;
    }

    public Boolean getShowGithub() {
        return showGithub;
    }

    public void setShowGithub(Boolean showGithub) {
        this.showGithub = showGithub;
    }

    public Boolean getShowLinkedin() {
        return showLinkedin;
    }

    public void setShowLinkedin(Boolean showLinkedin) {
        this.showLinkedin = showLinkedin;
    }
}
