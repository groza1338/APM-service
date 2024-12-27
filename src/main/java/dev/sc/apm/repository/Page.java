package dev.sc.apm.repository;

import java.util.List;

public record Page<E>(
        int page,
        int pageSize,
        long total,
        List<E> content
) {
}
