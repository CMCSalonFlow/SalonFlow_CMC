package com.example.salonflow.ai.knowledge;

import java.util.List;

public interface AiKnowledgeService {

    List<String> loadContext(Long branchId, String useCase, String query, int topK);
}

