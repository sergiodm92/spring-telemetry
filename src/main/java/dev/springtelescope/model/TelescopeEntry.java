package dev.springtelescope.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelescopeEntry {
    private String uuid;
    private TelescopeEntryType type;
    private LocalDateTime createdAt;
    private String batchId;
    private Map<String, Object> content;
    private String userIdentifier;
    private String tenantId;
    @Builder.Default
    private List<String> tags = new ArrayList<>();
}
