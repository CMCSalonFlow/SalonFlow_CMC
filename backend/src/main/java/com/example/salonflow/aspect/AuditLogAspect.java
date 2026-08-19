package com.example.salonflow.aspect;

import com.example.salonflow.dto.audit.CreateAuditLogRequest;
import com.example.salonflow.entity.enums.AuditAction;
import com.example.salonflow.security.CustomUserPrincipal;
import com.example.salonflow.services.service.AuditLogService;
import com.example.salonflow.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.List;

/**
 * Cách B (tự động): tự bắt mọi API ghi dữ liệu (POST/PUT/PATCH/DELETE)
 * trong toàn bộ các controller có tên dạng "Admin...Controller", không
 * cần sửa từng service. Log ghi ở đây mang tính "bắt hết, không sót",
 * nhưng old/new value chỉ là tham số thô (args của method), không có
 * ý nghĩa nghiệp vụ rõ ràng như log viết tay (Cách A).
 *
 * Với các nghiệp vụ nhạy cảm cần log rõ nghĩa (duyệt salon, refund,
 * xoá tài khoản...), xem thêm ví dụ log thủ công ở AuditLogUsageExamples.md
 * — Cách A và Cách B chạy song song, không loại trừ nhau (aspect này
 * vẫn ghi thêm 1 dòng log "OTHER" ngay cả khi service đã tự ghi log
 * chi tiết, chấp nhận trùng vì mục tiêu của Cách B là "không sót").
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogAspect {

    private final AuditLogService auditLogService;

    @Around("execution(* com.example.salonflow.controller.Admin*Controller.*(..)) && "
            + "(@annotation(org.springframework.web.bind.annotation.PostMapping) || "
            + "@annotation(org.springframework.web.bind.annotation.PutMapping) || "
            + "@annotation(org.springframework.web.bind.annotation.PatchMapping) || "
            + "@annotation(org.springframework.web.bind.annotation.DeleteMapping))")
    public Object autoAudit(ProceedingJoinPoint pjp) throws Throwable {
        // Để API chạy bình thường trước. Nếu API throw exception,
        // exception sẽ tự bay ra ngoài (không log, không nuốt lỗi)
        // vì GlobalExceptionHandler cần xử lý response như bình thường.
        Object result = pjp.proceed();

        try {
            writeAuditLog(pjp);
        } catch (Exception e) {
            // Log lỗi ghi audit không được làm ảnh hưởng response đã trả về
            log.warn("AuditLogAspect failed to write audit log", e);
        }

        return result;
    }

    private void writeAuditLog(ProceedingJoinPoint pjp) {
        String methodName = pjp.getSignature().getName();
        String controllerName = pjp.getTarget().getClass().getSimpleName();

        Long userId = null;
        String userEmail = null;
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserPrincipal principal) {
            userId = principal.getId();
            userEmail = principal.getUsername(); // fix: lấy email từ principal
        }

        String ip = null;
        var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            ip = SecurityUtil.getClientIp(request);
        }

        auditLogService.log(CreateAuditLogRequest.builder()
                .userId(userId)
                .userEmail(userEmail)
                .action(AuditAction.OTHER)
                .resourceType(controllerName)
                .resourceId(methodName)
                .newValue(sanitizeArgs(pjp.getArgs())) // fix: lọc args nhạy cảm
                .ipAddress(ip)
                .build());
    }

    /**
     * Lọc bỏ các object nhạy cảm khỏi args trước khi ghi log.
     * UserDetails chứa password hash — không được log ra.
     * HttpServletRequest/Response quá to và không cần thiết.
     */
    private Object sanitizeArgs(Object[] args) {
        if (args == null || args.length == 0) return null;

        List<Object> sanitized = Arrays.stream(args)
                .map(arg -> {
                    if (arg == null) return null;
                    // Bỏ UserDetails (chứa password hash)
                    if (arg instanceof UserDetails) return "[PRINCIPAL]";
                    // Bỏ Servlet objects (quá to, không cần)
                    if (arg instanceof jakarta.servlet.ServletRequest) return "[REQUEST]";
                    if (arg instanceof jakarta.servlet.ServletResponse) return "[RESPONSE]";
                    return arg;
                })
                .toList();

        // Nếu chỉ còn 1 arg sau khi lọc thì trả thẳng object đó (gọn hơn)
        return sanitized.size() == 1 ? sanitized.get(0) : sanitized;
    }
}