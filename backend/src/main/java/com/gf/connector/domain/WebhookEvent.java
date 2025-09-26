package com.gf.connector.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "webhook_events")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class WebhookEvent {
    @Id
    @GeneratedValue
    private UUID id;

    private String provider; // getnet
    private boolean processed;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = OffsetDateTime.now();
    }
}
