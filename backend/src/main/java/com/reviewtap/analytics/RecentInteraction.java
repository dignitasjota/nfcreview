package com.reviewtap.analytics;

import com.reviewtap.interaction.InteractionType;
import java.time.Instant;
import java.util.UUID;

public record RecentInteraction(Instant createdAt, UUID deviceId, String deviceName, String location,
        InteractionType type) {}
