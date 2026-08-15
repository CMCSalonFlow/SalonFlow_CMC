package com.example.salonflow.ai.service.impl;

import com.example.salonflow.ai.config.AiProperties;
import com.example.salonflow.ai.dto.chat.AiChatMessage;
import com.example.salonflow.ai.dto.chat.AiChatRequest;
import com.example.salonflow.ai.dto.chat.AiChatResponse;
import com.example.salonflow.ai.service.AiOrchestratorService;
import com.example.salonflow.ai.service.ConversationMemoryService;
import com.example.salonflow.services.service.BookingService;
import com.example.salonflow.repository.ServiceRepository;
import com.example.salonflow.entity.SalonService;
import com.example.salonflow.dto.booking.AvailabilityResponse;
import com.example.salonflow.dto.booking.CreateGuestBookingRequest;
import com.example.salonflow.dto.booking.BookingResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiOrchestratorServiceImpl implements AiOrchestratorService {

    @Qualifier("openAiWebClient")
    private final WebClient openAiWebClient;
    private final AiProperties aiProperties;
    private final ConversationMemoryService conversationMemoryService;
    private final BookingService bookingService;
    private final ServiceRepository serviceRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String MISUNDERSTANDING_PREFIX = "session:chatbot:misunderstanding:";

    @Override
    public AiChatResponse execute(AiChatRequest request) {
        String conversationId = request.conversationId();
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString();
        }

        log.info("Processing chatbot message for conversationId: {}", conversationId);

        // 1. Get history and append user message
        List<AiChatMessage> history = conversationMemoryService.getConversationHistory(conversationId);
        AiChatMessage userMsg = new AiChatMessage("user", request.message());
        conversationMemoryService.saveUserMessage(conversationId, userMsg);
        history.add(userMsg);

        // Check if already in fallback state before calling OpenAI
        String misunderstandingKey = MISUNDERSTANDING_PREFIX + conversationId;
        String countStr = redisTemplate.opsForValue().get(misunderstandingKey);
        int misunderstandingCount = countStr != null ? Integer.parseInt(countStr) : 0;
        int maxAttempts = aiProperties.getChatbot().getMaxFallbackAttempts();

        if (misunderstandingCount >= maxAttempts) {
            log.info("ConversationId {} is in fallback state", conversationId);
            return new AiChatResponse(
                    conversationId,
                    "Tôi không hiểu rõ yêu cầu của bạn. Vui lòng sử dụng biểu mẫu đặt lịch trực tiếp bên dưới.",
                    Collections.emptyList(),
                    true
            );
        }

        try {
            // Define tools definition
            List<Map<String, Object>> tools = getToolsDefinition();

            // Loop to handle tool execution
            String finalAnswer = null;
            boolean isMisunderstood = false;

            for (int turn = 0; turn < 5; turn++) { // Max 5 loop turns to prevent infinite loop
                List<Map<String, Object>> openAiMessages = buildOpenAiMessages(history);

                Map<String, Object> payload = new HashMap<>();
                payload.put("model", aiProperties.getChatbot().getModel());
                payload.put("temperature", 0.2);
                payload.put("messages", openAiMessages);
                payload.put("tools", tools);
                payload.put("response_format", Map.of("type", "json_object"));

                JsonNode root = openAiWebClient.post()
                        .uri("/chat/completions")
                        .header("Authorization", "Bearer " + aiProperties.getApiKey())
                        .bodyValue(payload)
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .block(Duration.ofSeconds(30));

                if (root == null) {
                    throw new RuntimeException("OpenAI returned null response");
                }

                JsonNode messageNode = root.path("choices").path(0).path("message");
                JsonNode toolCallsNode = messageNode.path("tool_calls");

                if (toolCallsNode.isArray() && !toolCallsNode.isEmpty()) {
                    log.info("OpenAI requested tool execution: {}", toolCallsNode);

                    // Save Assistant's tool request message to history
                    AiChatMessage toolCallMsg = new AiChatMessage("assistant", "TOOL_CALLS:" + objectMapper.writeValueAsString(toolCallsNode));
                    conversationMemoryService.saveAssistantMessage(conversationId, toolCallMsg);
                    history.add(toolCallMsg);

                    // Execute tool calls
                    for (JsonNode toolCall : toolCallsNode) {
                        String callId = toolCall.path("id").asText();
                        String funcName = toolCall.path("function").path("name").asText();
                        String argumentsStr = toolCall.path("function").path("arguments").asText();

                        String toolResult;
                        if ("search_availability".equals(funcName)) {
                            toolResult = handleSearchAvailability(request.branchId(), argumentsStr);
                        } else if ("create_booking".equals(funcName)) {
                            toolResult = handleCreateBooking(request.branchId(), argumentsStr);
                        } else {
                            toolResult = "{\"error\": \"Unknown tool\"}";
                        }

                        // Save tool execution result
                        AiChatMessage toolResultMsg = new AiChatMessage("tool", "TOOL_CALL_ID:" + callId + "|CONTENT:" + toolResult);
                        conversationMemoryService.saveUserMessage(conversationId, toolResultMsg);
                        history.add(toolResultMsg);
                    }
                    // Continue loop to send tool results to OpenAI
                } else {
                    // Chatbot conversational reply (No tool call)
                    String contentStr = messageNode.path("content").asText("");
                    log.info("OpenAI assistant final output: {}", contentStr);

                    if (!contentStr.isBlank()) {
                        try {
                            JsonNode jsonReply = objectMapper.readTree(contentStr);
                            finalAnswer = jsonReply.path("reply").asText();
                            isMisunderstood = jsonReply.path("isMisunderstood").asBoolean(false);
                        } catch (Exception e) {
                            // Fallback if AI didn't return json format despite response_format config
                            finalAnswer = contentStr;
                            isMisunderstood = false;
                        }
                    } else {
                        finalAnswer = "Rất tiếc, đã xảy ra lỗi trong quá trình xử lý yêu cầu của bạn.";
                    }

                    AiChatMessage assistantMsg = new AiChatMessage("assistant", contentStr);
                    conversationMemoryService.saveAssistantMessage(conversationId, assistantMsg);
                    break; // Exit loop
                }
            }

            // Handle misunderstanding logic
            if (isMisunderstood) {
                misunderstandingCount++;
                redisTemplate.opsForValue().set(misunderstandingKey, String.valueOf(misunderstandingCount), Duration.ofMinutes(30));
                log.warn("AI did not understand user. Misunderstanding count: {}/{}", misunderstandingCount, maxAttempts);
            } else {
                // Reset counter on successful understanding
                redisTemplate.delete(misunderstandingKey);
            }

            boolean needsHandoff = misunderstandingCount >= maxAttempts;
            if (needsHandoff) {
                finalAnswer = "Tôi không hiểu rõ yêu cầu của bạn. Vui lòng sử dụng biểu mẫu đặt lịch trực tiếp bên dưới.";
            }

            return new AiChatResponse(
                    conversationId,
                    finalAnswer,
                    Collections.emptyList(),
                    needsHandoff
            );

        } catch (Exception e) {
            log.error("Error executing Chatbot workflow", e);
            return new AiChatResponse(
                    conversationId,
                    "Có lỗi xảy ra khi trò chuyện với trợ lý ảo: " + e.getMessage(),
                    Collections.emptyList(),
                    false
            );
        }
    }

    private List<Map<String, Object>> getToolsDefinition() {
        return List.of(
            Map.of(
                "type", "function",
                "function", Map.of(
                    "name", "search_availability",
                    "description", "Tìm kiếm các khung giờ trống khả dụng của chi nhánh cho một dịch vụ cụ thể vào một ngày",
                    "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "service", Map.of(
                                "type", "string",
                                "description", "Tên dịch vụ cần tìm (ví dụ: 'Cắt tóc nam', 'Gội đầu')"
                            ),
                            "date_range", Map.of(
                                "type", "string",
                                "description", "Ngày cần tìm định dạng YYYY-MM-DD (ví dụ: '2026-08-16')"
                            )
                        ),
                        "required", List.of("service", "date_range")
                    )
                )
            ),
            Map.of(
                "type", "function",
                "function", Map.of(
                    "name", "create_booking",
                    "description", "Tạo đặt lịch làm tóc trực tiếp cho khách hàng",
                    "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "customerName", Map.of(
                                "type", "string",
                                "description", "Tên khách hàng đặt lịch"
                            ),
                            "customerPhone", Map.of(
                                "type", "string",
                                "description", "Số điện thoại liên hệ"
                            ),
                            "customerEmail", Map.of(
                                "type", "string",
                                "description", "Email khách hàng (nếu có)"
                            ),
                            "bookingDate", Map.of(
                                "type", "string",
                                "description", "Ngày hẹn định dạng YYYY-MM-DD"
                            ),
                            "startTime", Map.of(
                                "type", "string",
                                "description", "Giờ bắt đầu hẹn dạng HH:mm"
                            ),
                            "service", Map.of(
                                "type", "string",
                                "description", "Tên dịch vụ đặt lịch"
                            ),
                            "notes", Map.of(
                                "type", "string",
                                "description", "Ghi chú thêm từ khách hàng"
                            )
                        ),
                        "required", List.of("customerName", "customerPhone", "bookingDate", "startTime", "service")
                    )
                )
            )
        );
    }

    private List<Map<String, Object>> buildOpenAiMessages(List<AiChatMessage> history) {
        List<Map<String, Object>> openAiMessages = new ArrayList<>();
        // Add system instruction first
        openAiMessages.add(Map.of("role", "system", "content", aiProperties.getChatbot().getSystemPrompt()));

        for (AiChatMessage msg : history) {
            String content = msg.content();
            if (content.startsWith("TOOL_CALL_ID:")) {
                // Tool execution response format: TOOL_CALL_ID:<id>|CONTENT:<json>
                String raw = content.substring("TOOL_CALL_ID:".length());
                int pipeIdx = raw.indexOf("|CONTENT:");
                if (pipeIdx != -1) {
                    String toolCallId = raw.substring(0, pipeIdx);
                    String actualContent = raw.substring(pipeIdx + "|CONTENT:".length());
                    openAiMessages.add(Map.of(
                        "role", "tool",
                        "tool_call_id", toolCallId,
                        "content", actualContent
                    ));
                }
            } else if (content.startsWith("TOOL_CALLS:")) {
                // Tool call requests format: TOOL_CALLS:<json_array>
                String toolCallsJson = content.substring("TOOL_CALLS:".length());
                try {
                    JsonNode toolCallsNode = objectMapper.readTree(toolCallsJson);
                    openAiMessages.add(Map.of(
                        "role", "assistant",
                        "tool_calls", toolCallsNode
                    ));
                } catch (Exception e) {
                    openAiMessages.add(Map.of("role", "assistant", "content", content));
                }
            } else {
                openAiMessages.add(Map.of("role", msg.role(), "content", content));
            }
        }
        return openAiMessages;
    }

    private String handleSearchAvailability(Long branchId, String argumentsStr) {
        try {
            JsonNode args = objectMapper.readTree(argumentsStr);
            String serviceName = args.path("service").asText();
            String dateStr = args.path("date_range").asText();

            log.info("Executing tool search_availability: service={}, date={}", serviceName, dateStr);

            Long serviceId = findServiceIdByName(branchId, serviceName);
            if (serviceId == null) {
                return String.format("{\"error\": \"Không tìm thấy dịch vụ nào khớp với '%s'\"}", serviceName);
            }

            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            AvailabilityResponse response = bookingService.getAvailability(
                    branchId,
                    date,
                    List.of(serviceId),
                    null,
                    null
            );

            Map<String, Object> result = new HashMap<>();
            result.put("serviceId", serviceId);
            result.put("serviceName", serviceName);
            result.put("date", dateStr);
            result.put("availableSlots", response.getAvailableStartTimes());
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("Failed to execute tool search_availability", e);
            return String.format("{\"error\": \"%s\"}", e.getMessage());
        }
    }

    private String handleCreateBooking(Long branchId, String argumentsStr) {
        try {
            JsonNode args = objectMapper.readTree(argumentsStr);
            String customerName = args.path("customerName").asText();
            String customerPhone = args.path("customerPhone").asText();
            String customerEmail = args.path("customerEmail").asText(null);
            String bookingDateStr = args.path("bookingDate").asText();
            String startTimeStr = args.path("startTime").asText();
            String serviceName = args.path("service").asText();
            String notes = args.path("notes").asText(null);

            log.info("Executing tool create_booking: service={}, date={}, time={}", serviceName, bookingDateStr, startTimeStr);

            Long serviceId = findServiceIdByName(branchId, serviceName);
            if (serviceId == null) {
                return String.format("{\"error\": \"Không tìm thấy dịch vụ nào khớp với '%s' để đặt lịch\"}", serviceName);
            }

            LocalDate bookingDate = LocalDate.parse(bookingDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalTime startTime = LocalTime.parse(startTimeStr, DateTimeFormatter.ofPattern("HH:mm"));

            CreateGuestBookingRequest request = CreateGuestBookingRequest.builder()
                    .customerName(customerName)
                    .customerPhone(customerPhone)
                    .customerEmail(customerEmail)
                    .bookingDate(bookingDate)
                    .startTime(startTime)
                    .serviceIds(List.of(serviceId))
                    .notes(notes)
                    .build();

            BookingResponse response = bookingService.createGuestBooking(branchId, request);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("bookingId", response.getId());
            result.put("bookingCode", response.getId());
            result.put("totalPrice", response.getTotalPrice());
            result.put("dateTime", bookingDateStr + " " + startTimeStr);
            result.put("serviceName", serviceName);
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("Failed to execute tool create_booking", e);
            return String.format("{\"error\": \"Đặt lịch thất bại: %s\"}", e.getMessage());
        }
    }

    private Long findServiceIdByName(Long branchId, String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        List<SalonService> activeServices = serviceRepository.findByBranchIdAndIsActiveTrue(branchId);
        String target = name.toLowerCase().trim();

        // Pass 1: Exact Match or Substring match
        for (SalonService service : activeServices) {
            String sName = service.getName().toLowerCase();
            if (sName.equals(target) || sName.contains(target) || target.contains(sName)) {
                return service.getId();
            }
        }
        
        // Pass 2: Fuzzy keyword similarity (basic fallback)
        if (!activeServices.isEmpty()) {
            return activeServices.get(0).getId();
        }
        return null;
    }
}
