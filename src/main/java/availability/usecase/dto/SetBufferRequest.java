package availability.usecase.dto;

import java.time.Duration;

public record SetBufferRequest(String providerId, Duration buffer) {}
