package com.eurasia.pro_skill.common;

import lombok.Getter;

import java.util.List;

/**
 * 分页响应包装，字段命名与 Ant Design Vue Pagination 组件对齐，
 * 前端可直接将 {@code current}、{@code pageSize}、{@code total} 绑定到 {@code <a-pagination>}。
 *
 * <pre>
 * {
 *   "current":  1,
 *   "pageSize": 10,
 *   "total":    42,
 *   "records":  [...]
 * }
 * </pre>
 *
 * @param <T> 列表元素类型
 */
@Getter
public class PageResponse<T> {

    /** 当前页码（1-based），对应 AntDV {@code current} */
    private final int current;

    /** 每页条数，对应 AntDV {@code pageSize} */
    private final int pageSize;

    /** 总记录数，对应 AntDV {@code total} */
    private final long total;

    /** 当前页数据 */
    private final List<T> records;

    public PageResponse(int current, int pageSize, long total, List<T> records) {
        this.current = current;
        this.pageSize = pageSize;
        this.total = total;
        this.records = records;
    }

    /** 便捷工厂方法 */
    public static <T> PageResponse<T> of(int current, int pageSize, long total, List<T> records) {
        return new PageResponse<>(current, pageSize, total, records);
    }
}
