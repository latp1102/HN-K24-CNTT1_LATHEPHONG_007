package org.example.hackathon_de07.controller;

import org.example.hackathon_de07.model.dto.ChatRequest;
import org.example.hackathon_de07.model.dto.ChatResponse;
import org.example.hackathon_de07.service.FoodOrderService;
import org.example.hackathon_de07.service.RAGService;
import org.example.hackathon_de07.tools.RestaurantTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
class ChatConfig {
    @Bean
    public ChatMemory chatMemory() {
        return new ChatMemory() {
            private final Map<String, List<Message>> memory = new ConcurrentHashMap<>();

            @Override
            public void add(String conversationId, Message message) {
                memory.computeIfAbsent(conversationId, k -> new ArrayList<>()).add(message);
            }

            @Override
            public void add(String conversationId, List<Message> messages) {
                memory.computeIfAbsent(conversationId, k -> new ArrayList<>()).addAll(messages);
            }



            @Override
            public List<Message> get(String conversationId) {
                return memory.getOrDefault(conversationId, new ArrayList<>());
            }

            @Override
            public void clear(String conversationId) {
                memory.remove(conversationId);
            }
        };
    }
}

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatClient chatClient;
    private final RestaurantTools restaurantTools;
    private final FoodOrderService foodOrderService;
    private final RAGService ragService;

    private final ChatMemory chatMemory;

    public ChatController(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory, RestaurantTools restaurantTools, FoodOrderService foodOrderService, RAGService ragService) {
        this.restaurantTools = restaurantTools;
        this.foodOrderService = foodOrderService;
        this.ragService = ragService;
        this.chatMemory = chatMemory;
        this.chatClient = chatClientBuilder
                .defaultSystem("Bạn là một trợ lý ảo thân thiện của nhà hàng. " +
                        "Nhiệm vụ của bạn là giải đáp các thắc mắc về nhà hàng, giới thiệu món ăn, và hỗ trợ khách hàng đặt đồ ăn. " +
                        "Luôn kiểm tra kỹ các thông tin từ công cụ (như giá cả, tồn kho, tên món). " +
                        "Khi khách muốn đặt món, hãy yêu cầu đầy đủ số điện thoại, tên, địa chỉ và món ăn muốn đặt.")
                .defaultTools(restaurantTools, foodOrderService, ragService)
                .build();
    }

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {
        List<Message> history = chatMemory.get(request.getSessionId());
        List<Message> promptMessages = new ArrayList<>(history);
        UserMessage userMsg = new UserMessage(request.getMessage());
        promptMessages.add(userMsg);

        String responseContent = chatClient.prompt()
                .messages(promptMessages)
                .call()
                .content();

        chatMemory.add(request.getSessionId(), userMsg);
        chatMemory.add(request.getSessionId(), new AssistantMessage(responseContent));

        return new ChatResponse(responseContent);
    }
}
