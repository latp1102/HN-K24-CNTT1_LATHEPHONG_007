package org.example.hackathon_de07.tools;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.hackathon_de07.model.entity.FoodItem;
import org.example.hackathon_de07.repository.FoodItemRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class RestaurantTools {

    private final FoodItemRepository foodItemRepository;

    @Tool(description = "Tìm kiếm thông tin các món ăn trong nhà hàng bằng tên hoặc từ khóa món ăn. Trả về thông tin tên món, giá, tồn kho và danh mục của từng món ăn.")
    public String searchFoodByName(String keyword) {
        List<FoodItem> items = foodItemRepository.findByNameContainingIgnoreCase(keyword);
        if (items.isEmpty()) {
            return "Không tìm thấy món ăn nào có chứa từ khóa: " + keyword;
        }
        return formatFoodItems(items);
    }

    @Tool(description = "Tìm kiếm tất cả các món ăn thuộc một danh mục (ví dụ: Khai vị, Món chính, Đồ uống...). Trả về danh sách món ăn, giá và tồn kho của chúng.")
    public String searchFoodByCategory(String foodCategoryName) {
        List<FoodItem> items = foodItemRepository.findByFoodFoodCategoryNameContainingIgnoreCase(foodCategoryName);
        if (items.isEmpty()) {
            return "Không tìm thấy món ăn nào thuộc danh mục: " + foodCategoryName;
        }
        return formatFoodItems(items);
    }

    private String formatFoodItems(List<FoodItem> items) {
        return items.stream()
                .map(item -> String.format("- Món: %s | Giá: %s | Tồn kho: %d | Danh mục: %s",
                        item.getName(), item.getPrice(), item.getStock(), item.getFoodFoodCategory().getName()))
                .collect(Collectors.joining("\n"));
    }
}
