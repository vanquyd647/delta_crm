package dentalbackend.controller;

import dentalbackend.domain.*;
import dentalbackend.dto.PrescriptionDTO;
import dentalbackend.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prescriptions")
public class PrescriptionController {
    private final PrescriptionRepository prescriptionRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;

    public PrescriptionController(PrescriptionRepository prescriptionRepository,
                                  AppointmentRepository appointmentRepository,
                                  UserRepository userRepository) {
        this.prescriptionRepository = prescriptionRepository;
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<PrescriptionDTO> get(@PathVariable Long id) {
        return prescriptionRepository.findById(id)
                .map(p -> {
                    PrescriptionDTO dto = PrescriptionDTO.builder()
                            .id(p.getId())
                            .appointmentId(p.getAppointment() != null ? p.getAppointment().getId() : null)
                            .patientId(p.getPatient() != null ? p.getPatient().getId() : null)
                            .doctorId(p.getDoctor() != null ? p.getDoctor().getId() : null)
                            .content(p.getContent())
                            .build();
                    return ResponseEntity.ok(dto);
                }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Prescription>> list() {
        return ResponseEntity.ok(prescriptionRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<PrescriptionDTO> create(@RequestBody PrescriptionDTO dto) {
        Prescription p = new Prescription();
        p.setAppointment(dto.appointmentId != null ? appointmentRepository.findById(dto.appointmentId).orElse(null) : null);
        p.setPatient(dto.patientId != null ? userRepository.findById(dto.patientId).orElse(null) : null);
        p.setDoctor(dto.doctorId != null ? userRepository.findById(dto.doctorId).orElse(null) : null);
        p.setContent(dto.content);
        Prescription saved = prescriptionRepository.save(p);
        dto.id = saved.getId();
        return ResponseEntity.ok(dto);
    }
}
