package dev.sc.apm.repository;

import jakarta.validation.constraints.Positive;

public record Pageable(
        @Positive int page,
        @Positive int size
) {
}
