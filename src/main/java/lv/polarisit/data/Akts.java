package lv.polarisit.data;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Getter
@Setter
public class Akts {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotEmpty
    @Column(name = "person_name")
    private String personName = "";

    @NotNull
    @Column(name = "doc_date")
    private LocalDate docDate = LocalDate.now();

    @NotNull
    @Column(name = "doc_amount", precision = 19, scale = 2)
    private BigDecimal docAmount;

    @NotEmpty
    @Column(name = "receipt_image_path")
    private String receiptImagePath;


}
