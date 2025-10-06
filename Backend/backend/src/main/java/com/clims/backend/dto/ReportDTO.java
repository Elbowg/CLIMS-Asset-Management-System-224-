package com.clims.backend.dto;

import java.time.LocalDateTime;

public class ReportDTO {
    private Long id;
    private Long generatedById;
    private String content;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getGeneratedById() { return generatedById; }
    public void setGeneratedById(Long generatedById) { this.generatedById = generatedById; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
