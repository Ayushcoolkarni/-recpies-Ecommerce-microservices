package Ecom.recipe_service.exception;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder
public class ErrorResponse {
    private int status;
    private String message;
    private LocalDateTime timestamp;
}