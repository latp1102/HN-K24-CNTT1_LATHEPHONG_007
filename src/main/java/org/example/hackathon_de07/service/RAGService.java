package org.example.hackathon_de07.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RAGService {

    private final VectorStore vectorStore;

    @Tool(description = "Tra cứu các thông tin về nhà hàng như địa chỉ, giờ hoạt động, chính sách giao hàng, thanh toán, liên hệ, v.v.")
    public String getRestaurantInfo(String question) {
        List<Document> documents = vectorStore.similaritySearch(
            SearchRequest.builder().query(question).topK(3).build()
        );
        
        if (documents.isEmpty()) {
            return "Không tìm thấy thông tin nhà hàng phù hợp với câu hỏi.";
        }
        
        return documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));
    }
}
