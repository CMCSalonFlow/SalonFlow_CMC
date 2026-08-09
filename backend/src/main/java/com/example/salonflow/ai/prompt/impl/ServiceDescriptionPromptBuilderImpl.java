package com.example.salonflow.ai.prompt.impl;

import com.example.salonflow.ai.config.AiProperties;
import com.example.salonflow.ai.dto.description.ServiceDescriptionGenerateRequest;
import com.example.salonflow.ai.prompt.ServiceDescriptionPromptBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ServiceDescriptionPromptBuilderImpl implements ServiceDescriptionPromptBuilder {

    private final AiProperties aiProperties;

    @Override
    public String buildPrompt(ServiceDescriptionGenerateRequest request) {
        AiProperties.ServiceDescriptionProperties props = aiProperties.getServiceDescription();
        int minWords = props != null && props.getMinWords() != null ? props.getMinWords() : 100;
        int maxWords = props != null && props.getMaxWords() != null ? props.getMaxWords() : 150;

        List<String> keywords = request.keywords().stream()
                .filter(keyword -> keyword != null && !keyword.isBlank())
                .map(String::trim)
                .toList();

        String keywordList = String.join(", ", keywords);

        return """
                Nhiệm vụ:
                Viết mô tả dịch vụ cho salon/spa bằng tiếng Việt, tối ưu SEO tự nhiên.

                Thông tin đầu vào:
                - Salon ID: %d
                - Tên dịch vụ: %s
                - Từ khóa: %s

                Yêu cầu đầu ra:
                - Chỉ trả về phần mô tả cuối cùng, không tiêu đề, không markdown, không giải thích.
                - Độ dài từ %d đến %d từ.
                - Giọng văn sang trọng, chuyên nghiệp, thân thiện, phù hợp khách hàng salon/spa.
                - Chèn các từ khóa một cách tự nhiên, không nhồi nhét.
                - Nhấn mạnh lợi ích, trải nghiệm, cảm giác, và giá trị mà dịch vụ mang lại.
                - Nếu phù hợp, có thể gợi cảm giác thư giãn, chăm sóc, làm đẹp và tin cậy.
                """.formatted(
                        request.salonId(),
                        request.serviceName().trim(),
                        keywordList,
                        minWords,
                        maxWords
                );
    }
}
