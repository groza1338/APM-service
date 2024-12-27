package dev.sc.apm.service;

import dev.sc.apm.dto.PageResponseDto;
import dev.sc.apm.repository.Page;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class ServiceUtil {
    public static <D, E> PageResponseDto<D> getPageResponse(
            Supplier<Page<E>> entityProvider,
            Function<E, D> mapper
    ) {
        Page<E> page = entityProvider.get();

        List<D> content = page.content().stream()
                .map(mapper)
                .toList();

        return new PageResponseDto<>(
                page.page(),
                page.pageSize(),
                page.total(),
                content
        );
    }
}
