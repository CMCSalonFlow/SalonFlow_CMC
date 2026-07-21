package com.example.salonflow.ai.safety;

public interface AiSafetyPolicy {

    void validateInput(String text);

    void validateOutput(String text);
}

