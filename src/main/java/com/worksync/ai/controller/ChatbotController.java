package com.worksync.ai.controller;


import com.worksync.ai.model.dto.ChatbotRequest;
import com.worksync.ai.model.dto.ChatbotResponse;
import com.worksync.ai.response.ApiResponse;
import com.worksync.ai.service.ChatbotService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    @Autowired
    private ChatbotService chatbotService;

    @PostMapping("/query")
    public ResponseEntity<ApiResponse<ChatbotResponse>> query(
            @Valid @RequestBody ChatbotRequest request) {
        try {
            ChatbotResponse response = chatbotService.processQuery(request);
            
            return ResponseEntity.ok(new ApiResponse<>(
                response.isSuccess() ? "SUCCESS" : "ERROR",
                response.getMessage(),
                response
            ));
            
        } catch (Exception e) {
            log.error("Error processing chatbot query: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                "ERROR",
                "Failed to process query: " + e.getMessage(),
                null
            ));
        }
    }
} 