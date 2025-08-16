package com.itzi.itzi.recruitings.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecruitingAiGenerateRequest {

    private MultipartFile postImage;
    private String title;
    private String target;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private String benefit;
    private String condition;

    private Boolean targetNegotiable;
    private Boolean periodNegotiable;
    private Boolean benefitNegotiable;
    private Boolean conditionNegotiable;

    private LocalDate exposureEndDate;

}
