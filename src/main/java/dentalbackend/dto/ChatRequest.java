package dentalbackend.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class ChatRequest {
    @NotBlank(message = "message must not be blank")
    private String message;

    // (tuỳ chọn) cho phép truyền system prompt nhẹ
    private String system;
}
