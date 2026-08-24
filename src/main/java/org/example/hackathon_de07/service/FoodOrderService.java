package org.example.hackathon_de07.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.hackathon_de07.model.constant.FoodOrderStatus;
import org.example.hackathon_de07.model.entity.Diner;
import org.example.hackathon_de07.model.entity.FoodFoodOrderItem;
import org.example.hackathon_de07.model.entity.FoodItem;
import org.example.hackathon_de07.model.entity.FoodOrder;
import org.example.hackathon_de07.repository.DinerRepository;
import org.example.hackathon_de07.repository.FoodFoodOrderItemRepository;
import org.example.hackathon_de07.repository.FoodItemRepository;
import org.example.hackathon_de07.repository.FoodOrderRepository;
import org.example.hackathon_de07.model.dto.OrderItemRequest;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FoodOrderService {

    private final DinerRepository dinerRepository;
    private final FoodItemRepository foodItemRepository;
    private final FoodOrderRepository foodOrderRepository;
    private final FoodFoodOrderItemRepository foodFoodOrderItemRepository;



    @Tool(description = "Tạo một đơn đặt hàng đồ ăn mới cho khách. Yêu cầu danh sách các món ăn, số lượng mỗi món, và thông tin người đặt hàng (số điện thoại, tên, địa chỉ). Trả về kết quả thành công hoặc thông báo lỗi nếu không đủ tồn kho.")
    @Transactional
    public String createFoodOrder(String dinerPhone, String dinerName, String address, List<OrderItemRequest> items) {
        try {
            Diner diner = dinerRepository.findByPhone(dinerPhone)
                    .orElseGet(() -> {
                        Diner newDiner = new Diner();
                        newDiner.setPhone(dinerPhone);
                        newDiner.setFullName(dinerName);
                        newDiner.setAddress(address);
                        return dinerRepository.save(newDiner);
                    });

            BigDecimal totalAmount = BigDecimal.ZERO;
            List<FoodFoodOrderItem> orderItems = new ArrayList<>();
            List<FoodItem> itemsToUpdate = new ArrayList<>();

            for (OrderItemRequest req : items) {
                Optional<FoodItem> foodItemOpt = foodItemRepository.findByNameIgnoreCase(req.getFoodName());
                if (foodItemOpt.isEmpty()) {
                    return "Lỗi: Không tìm thấy món ăn '" + req.getFoodName() + "'. Vui lòng kiểm tra lại tên món.";
                }

                FoodItem foodItem = foodItemOpt.get();
                if (foodItem.getStock() < req.getQuantity()) {
                    return "Lỗi: Món '" + req.getFoodName() + "' không đủ số lượng (chỉ còn " + foodItem.getStock() + "). Đơn hàng đã bị hủy.";
                }

                foodItem.setStock(foodItem.getStock() - req.getQuantity());
                itemsToUpdate.add(foodItem);

                BigDecimal lineTotal = foodItem.getPrice().multiply(BigDecimal.valueOf(req.getQuantity()));
                totalAmount = totalAmount.add(lineTotal);

                FoodFoodOrderItem orderItem = new FoodFoodOrderItem();
                orderItem.setFoodItem(foodItem);
                orderItem.setQuantity(req.getQuantity());
                orderItem.setUnitPrice(foodItem.getPrice());
                
                orderItems.add(orderItem);
            }

            foodItemRepository.saveAll(itemsToUpdate);

            FoodOrder order = new FoodOrder();
            order.setDiner(diner);
            order.setFoodFoodOrderDate(LocalDateTime.now());
            order.setStatus(FoodOrderStatus.PENDING);
            order.setTotalAmount(totalAmount);
            order.setNote("Đặt qua AI Chatbot");
            
            FoodOrder savedOrder = foodOrderRepository.save(order);

            for (FoodFoodOrderItem orderItem : orderItems) {
                orderItem.setFoodFoodOrder(savedOrder);
            }
            foodFoodOrderItemRepository.saveAll(orderItems);

            return "Đã đặt hàng thành công! Mã đơn: " + savedOrder.getId() + " - Tổng tiền: " + totalAmount + " VND.";

        } catch (Exception e) {
            log.error("Lỗi khi tạo đơn hàng: ", e);
            throw new RuntimeException("Đã xảy ra lỗi hệ thống khi xử lý đơn hàng. Vui lòng thử lại sau.", e);
        }
    }
}
