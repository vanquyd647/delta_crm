package dentalbackend.dto;

import lombok.Data;

@Data
public class UpdatePreferencesRequest {
    private String themePreference;        // "dark" | "light"
    private String languagePreference;     // "vi" | "en"
    private String notificationPreference; // "EMAIL,SMS,PUSH"
}