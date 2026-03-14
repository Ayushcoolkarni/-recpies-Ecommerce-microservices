package Ecom.order_service.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class OrderRequest {
    private Long userId;
    private Long addressId;
    private List<OrderItemRequest> items;
}