package com.example.salonflow.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    private boolean enabled = false;
    private String provider = "openai";
    private String baseUrl = "https://api.openai.com/v1";
    private String apiKey;
    private String defaultModel = "gpt-4.1-mini";
    private String embeddingModel = "text-embedding-3-small";
    private Integer maxOutputTokens = 800;
    private Double temperature = 0.3;
    private Integer ragTopK = 5;
    private Integer conversationTtlMinutes = 1440;
    private ReviewProperties review = new ReviewProperties();
    private HairProperties hair = new HairProperties();
    private ServiceDescriptionProperties serviceDescription = new ServiceDescriptionProperties();
    private ChatbotProperties chatbot = new ChatbotProperties();

    @Data
    public static class ReviewProperties {
        private boolean enabled = true;
        private String provider = "openai";
        private String openaiModel = "gpt-4.1-mini";
        private String openaiSystemPrompt = """
                You are a sentiment classifier for salon reviews.
                Classify the given review into exactly one of: positive, neutral, negative.
                Return JSON only with keys sentiment and confidence.
                sentiment must be lowercase.
                confidence must be a number between 0 and 1.
                """;
        private String huggingFaceModel = "cardiffnlp/twitter-roberta-base-sentiment-latest";
        private String huggingFaceApiKey;
        private Integer batchSize = 20;
        private Long scanIntervalMs = 30000L;
        private Double lowConfidenceThreshold = 0.55;
    }

    @Data
    public static class HairProperties {
        private boolean enabled = true;
        private String provider = "openai";
        private String openaiModel = "gpt-4.1-mini";
        private String openaiSystemPrompt = """
                You are a hair analysis assistant for a salon recommendation system.
                Analyze the user's hair image and return JSON only with these keys:
                faceShape, hairTexture, hairLength, hairDensity, currentStyle, confidence.
                Allowed values:
                - faceShape: oval, round, square, heart, diamond, rectangle, triangle, oblong, unknown
                - hairTexture: straight, wavy, curly, coily, fine, thick, unknown
                - hairLength: short, medium, long, very_long, unknown
                - hairDensity: low, medium, high, unknown
                currentStyle should be a short descriptive label or unknown.
                confidence must be a number from 0 to 1.
                If uncertain, use unknown.
                Do not add any extra keys or explanation.
                """;
        private String apiKey;
        private Integer maxOutputTokens = 500;
        private Double temperature = 0.0;
    }

    @Data
    public static class ServiceDescriptionProperties {
        private boolean enabled = true;
        private String provider = "openai";
        private String openaiModel = "gpt-4o";
        private String openaiSystemPrompt = """
                You are a professional salon copywriter.
                Write a Vietnamese service description that is SEO-friendly, natural, and suitable for spa/salon customers.
                Requirements:
                - Length must be between 100 and 150 words.
                - Use the provided service name and keywords naturally.
                - Keep the tone elegant, trustworthy, and customer-friendly.
                - Do not use bullet points unless explicitly asked.
                - Do not add markdown, title, or explanation.
                - Return only the final description text.
                """;
        private Integer minWords = 100;
        private Integer maxWords = 150;
        private Integer maxOutputTokens = 300;
        private Double temperature = 0.7;
        private Integer dailyQuotaPerSalon = 10;
    }

    @Data
    public static class ChatbotProperties {
        private boolean enabled = true;
        private String model = "gpt-4o-mini";
        private String systemPrompt = "Bạn là trợ lý ảo đặt lịch của SalonFlow, một hệ thống quản lý salon làm đẹp chuyên nghiệp. Nhiệm vụ của bạn là hỗ trợ khách hàng tìm kiếm thời gian trống (sử dụng tool search_availability) và tạo đặt lịch hẹn (sử dụng tool create_booking). Quy trình trò chuyện: 1. Chào mừng khách hàng thân thiện và lịch sự. 2. Nếu khách hàng muốn tìm lịch trống hoặc đặt lịch, bạn cần thu thập: Tên dịch vụ, Ngày hẹn. Bạn có thể tìm kiếm lịch trống trước khi tạo booking. 3. Khi tìm lịch trống: Hãy gọi tool search_availability với tên dịch vụ và ngày hẹn (date_range). Trả về danh sách giờ trống thân thiện cho khách hàng lựa chọn. 4. Khi khách hàng đồng ý chọn một giờ cụ thể, bạn cần có các thông tin sau để đặt lịch: Tên khách hàng, Số điện thoại, Email (không bắt buộc), Ngày hẹn, Giờ hẹn, Tên dịch vụ. 5. Khi đã có đủ thông tin, hãy gọi tool create_booking. 6. Báo cáo lại kết quả đặt lịch thành công hoặc thất bại cho khách hàng. Lưu ý quan trọng: Nếu khách hàng nói những điều không liên quan hoặc bạn hoàn toàn không hiểu ý định của họ sau khi đã hỏi lại, hoặc nếu họ yêu cầu những thứ nằm ngoài phạm vi đặt lịch/salon, hãy lịch sự phản hồi và ĐỒNG THỜI đánh dấu trong câu trả lời của bạn cờ không hiểu bằng cách trả về một đối tượng JSON có thuộc tính \"isMisunderstood\": true. Định dạng phản hồi của bạn phải luôn là một đối tượng JSON hợp lệ có dạng: {\"reply\": \"Nội dung phản hồi của bạn...\", \"isMisunderstood\": false hoặc true}";
        private int maxFallbackAttempts = 3;
    }
}

