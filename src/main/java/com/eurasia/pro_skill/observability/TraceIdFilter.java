package com.eurasia.pro_skill.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 给一次请求一个号，写进日志和响应头。
 * app.lesson.trace-id=off 时不写，日志里的括号是空的。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Trace-Id";
    public static final String MDC_KEY = "traceId";

    private static final Logger log = LoggerFactory.getLogger(TraceIdFilter.class);

    private final boolean enabled;

    public TraceIdFilter(@Value("${app.lesson.trace-id:off}") String traceIdSwitch) {
        this.enabled = "on".equalsIgnoreCase(traceIdSwitch);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!enabled) { // app.lesson.trace-id=off：不写号，日志括号是空的
            filterChain.doFilter(request, response);
            return;
        }

        String traceId = request.getHeader(HEADER); // 调用方可以自己带 X-Trace-Id
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString(); // 没带就生成一个，这一次请求专用
        }
        MDC.put(MDC_KEY, traceId); // 放进 MDC。日志格式里的 %X{traceId} 就是取这个键
        response.setHeader(HEADER, traceId); // 同一个号写回响应头，页面上也能看到
        try {
            log.info("request {} {}", request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response); // 后面的 Controller、Service 打的日志都带着这个号
        } finally {
            MDC.remove(MDC_KEY); // 请求结束清掉，避免串到下一个请求
        }
    }
}
