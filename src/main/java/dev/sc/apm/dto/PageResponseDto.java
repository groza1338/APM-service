package dev.sc.apm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class PageResponseDto<V> {
    int page;
    int pageSize;
    long total;
    List<V> content;
}
