package com.learnify.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "lesson")
@Getter
@Setter
@NoArgsConstructor
public class Lesson {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "objectives", columnDefinition = "text[]")
    private List<String> objectives = new ArrayList<>();

    // Open-schema content blocks (headings/paragraphs/code/video/mcq/...);
    // validated at the application layer, not the database, per the
    // "fully open JSON content" decision.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<Map<String, Object>> content = new ArrayList<>();

    @Column(name = "is_enriched", nullable = false)
    private boolean isEnriched = false;

    @Column(nullable = false)
    private boolean completed = false;

    @Column(nullable = false)
    private boolean bookmarked = false;

    // Cached Hinglish translation + synthesized audio (see LessonAudioService) —
    // both null until the first "Listen in Hinglish" request for this lesson,
    // then reused forever after so repeat requests cost zero Gemini calls.
    @Column(name = "hinglish_text")
    private String hinglishText;

    @Column(name = "hinglish_audio")
    private byte[] hinglishAudio;

    @ManyToOne(optional = false)
    @JoinColumn(name = "module_id", nullable = false)
    private Module module;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
