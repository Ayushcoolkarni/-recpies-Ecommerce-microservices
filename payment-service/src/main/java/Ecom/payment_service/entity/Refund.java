package Ecom.payment_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "refunds")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long paymentId;
    private Double amount;
    private String reason;
    private String status;
    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() { processedAt = LocalDateTime.now(); }
}