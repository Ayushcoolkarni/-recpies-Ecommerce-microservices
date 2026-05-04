 
package Ecom.user_service.dto.request; 
 
import jakarta.validation.constraints.Email; 
import jakarta.validation.constraints.NotBlank; 
import lombok.Data; 
 
@Data 
public class OtpSendRequest { 
    @NotBlank @Email 
    private String email; 
} 
