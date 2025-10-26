package dentalbackend.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PrescriptionDTO {
    public Long id;
    public Long appointmentId;
    public Long patientId;
    public Long doctorId;
    public String content;
}

