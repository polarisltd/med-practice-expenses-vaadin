package lv.polarisit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;


@Service
@Scope("singleton")
@Getter
@Setter
@Component
public class PropertyHolder {
    @Value("${image.path}")
    private String imagePath;
    @Value("${word-template.path}")
    private String wordTemplatePath;
    @Value("${word-output.path}")
    private String wordOutputPath;






}