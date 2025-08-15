package com.itzi.itzi.recruitings.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecruitingAiGenerateRequest {

    private String postImage;
    private String title;
    private String target;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private String benefit;
    private String condition;

    @Valid
    private Negotiables negotiables;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Negotiables {
        private Boolean target;
        private Boolean period;
        private Boolean benefit;
        private Boolean condition;
    }

}
