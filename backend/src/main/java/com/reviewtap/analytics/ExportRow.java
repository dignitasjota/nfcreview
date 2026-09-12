package com.reviewtap.analytics;

import com.reviewtap.interaction.InteractionType;
import java.time.Instant;

public record ExportRow(Instant createdAt, String deviceName, String location, InteractionType type) {}
