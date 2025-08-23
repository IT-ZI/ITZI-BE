package com.itzi.itzi.promotion.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.itzi.itzi.posts.domain.Type;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Getter
@Setter
public class BenefitGenerateAiRequest {

    private Type type;

    private MultipartFile image;
    private String title;
    private String target;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    private String benefit;
    private String condition;
    private LocalDate exposureEndDate;

}
