package dev.springtelescope.storage.jpa;

import dev.springtelescope.model.TelescopeEntryType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "telescope_entries", indexes = {
        @Index(name = "idx_telescope_created_at", columnList = "createdAt"),
        @Index(name = "idx_telescope_batch_id", columnList = "batchId"),
        @Index(name = "idx_telescope_user", columnList = "userIdentifier"),
        @Index(name = "idx_telescope_tenant", columnList = "tenantId"),
        @Index(name = "idx_telescope_type", columnList = "type")
})
public class TelescopeEntryEntity {

    @Id
    @Column(length = 36)
    private String uuid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TelescopeEntryType type;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(length = 36)
    private String batchId;

    @Column(columnDefinition = "TEXT")
    private String contentJson;

    @Column(length = 255)
    private String userIdentifier;

    @Column(length = 255)
    private String tenantId;

    @Column(length = 2000)
    private String tagsJson;

    public TelescopeEntryEntity() {
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public TelescopeEntryType getType() {
        return type;
    }

    public void setType(TelescopeEntryType type) {
        this.type = type;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getContentJson() {
        return contentJson;
    }

    public void setContentJson(String contentJson) {
        this.contentJson = contentJson;
    }

    public String getUserIdentifier() {
        return userIdentifier;
    }

    public void setUserIdentifier(String userIdentifier) {
        this.userIdentifier = userIdentifier;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getTagsJson() {
        return tagsJson;
    }

    public void setTagsJson(String tagsJson) {
        this.tagsJson = tagsJson;
    }
}
