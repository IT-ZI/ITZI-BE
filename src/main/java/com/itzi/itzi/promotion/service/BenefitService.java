package com.itzi.itzi.promotion.service;

import com.itzi.itzi.auth.domain.User;
import com.itzi.itzi.auth.repository.UserRepository;
import com.itzi.itzi.global.api.code.ErrorStatus;
import com.itzi.itzi.global.exception.GeneralException;
import com.itzi.itzi.global.gemini.GeminiService;
import com.itzi.itzi.global.s3.S3Service;
import com.itzi.itzi.posts.domain.Post;
import com.itzi.itzi.posts.domain.Status;
import com.itzi.itzi.posts.domain.Type;
import com.itzi.itzi.posts.repository.PostRepository;
import com.itzi.itzi.promotion.dto.request.BenefitGenerateAiRequest;
import com.itzi.itzi.promotion.dto.response.BenefitGenerateAiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;

@Service
@Slf4j
@RequiredArgsConstructor
public class BenefitService {

    private final PostRepository postRepository;
    private final S3Service s3Service;
    private final GeminiService geminiService;
    private final UserRepository userRepository;

    // 혜택 홍보 게시글 상세 정보 AI 반환
    public BenefitGenerateAiResponse generateBenefitAi(Long userId, Type type, BenefitGenerateAiRequest request) {

        // 1. 검증 : 날짜 역전 금지, 모든 필드 작성
        validate(request);

        // 2. 프롬프트 구성
        String prompt = buildPrompt(type, request);

        // 3. Gemini 호출
        String content = geminiService.callGemini(prompt);

        // 4. 엔티티 구성
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND));

        Post post = Post.builder()
                .user(user)
                .type(type)
                .title(request.getTitle())
                .target(request.getTarget())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .benefit(request.getBenefit())
                .condition(request.getCondition())
                .content(content)
                .exposureEndDate(request.getExposureEndDate())
                .status(Status.DRAFT)
                .build();

        // 5. 이미지 업로드 및 변경
        handleImageUpload(post, request.getImage());

        // 6. 저장
        Post savedPost = postRepository.save(post);

        // 7. 응답 DTO
        return BenefitGenerateAiResponse.builder()
                .postId(savedPost.getPostId())
                .userId(userId)
                .type(savedPost.getType())
                .status(savedPost.getStatus())
                .image(savedPost.getPostImage())
                .title(savedPost.getTitle())
                .target(savedPost.getTarget())
                .startDate(savedPost.getStartDate())
                .endDate(savedPost.getEndDate())
                .benefit(savedPost.getBenefit())
                .condition(savedPost.getCondition())
                .content(savedPost.getContent())
                .exposureEndDate(savedPost.getExposureEndDate())
                .build();

    }

    private void validate(BenefitGenerateAiRequest request) {

        LocalDate start = request.getStartDate();
        LocalDate end = request.getEndDate();

        // 두 날짜가 모두 존재할 때만 비교
        if (start != null && end != null) {
            if (end.isBefore(start)) {
                throw new GeneralException(ErrorStatus.DATE_RANGE_INVALID);}
        }

        // 모든 텍스트 필드 필수
        if (!hasText(request.getTitle())
                || !hasText(request.getTarget())
                || !hasText(request.getBenefit())
                || !hasText(request.getCondition())) {
            throw new GeneralException(ErrorStatus.REQUIRED_FIELD_MISSING);
        }
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private String buildPrompt(Type type, BenefitGenerateAiRequest r) {

        // MultipartFile 기준으로 존재 여부만 판단
        String postImageLine = (r.getImage() != null && !r.getImage().isEmpty())
                ? "\n(이미지 첨부됨)"
                : "";

        // 제목, 타깃에서 학교명 자동 추출
        String school = geminiService.extractSchoolName(r.getTitle())
                .or(() -> geminiService.extractSchoolName(r.getTarget()))
                .orElse("00대학교");       // 기본값

        return """
        너는 아래 '원본 텍스트'를 참고하여, 명시된 규칙에 따라 빈칸을 채우거나 일부 문구를 수정하여 최종 본문만 출력한다.
        - 이모지 사용 규칙
            1) 이모지 리스트 `[☺️😊😚🙌🏻🤝🏻🤙🏻🙏🏻🍀⭐️💌📍❗️️💬📢🕒]`에서 **3개를 무작위로 선택**
            2) **1문단과 2문단 본문 내용에서만** 적절히 배치
            3) 이모지는 문장 중간에 삽입 금지
            4) 📅 제휴 기간, 🎯 제휴 대상, 💬 문의 안내 **섹션과 해당 섹션의 본문에는 이모지 사용 금지**
        - 불필요한 접두/접미 문장, 설명, 따옴표, 코드블록 금지
        - 300~500자 내외, 문단은 샘플처럼 2개 본문 + 3개 섹션으로 구성
        - 아래 값으로 빈칸을 치환하여 최종 본문만 출력
        
        [입력 값]
        - 제목: %s
        - 대상: %s
        - 기간: %s ~ %s
        - 혜택: %s
        
        [원본 텍스트]
        [%s]
        
        저희는 %s분들을 위해 특별한 이벤트를 준비했습니다.
        %s 동안, 저희 매장을 방문하시는 %s분들께 %s 혜택을 제공합니다.
        
        이번 이벤트는 저희 매장을 아껴주시는 고객님들께 보답하고자 마련했습니다.
        친구, 가족, 혹은 연인과 함께 방문하셔서 저희가 준비한 혜택을 마음껏 누리시고 즐거운 추억을 만들어 가세요!
        궁금한 점은 언제든 편하게 문의해주세요.
        
        📅 이벤트 기간
        %s ~ %s
        
        🎯 이벤트 대상
        %s
        
        💬 문의 안내
        문의는 매장으로 직접 방문하시거나, 아래 연락처로 문의해주시기 바랍니다.
        %s
        """.formatted(
            // [입력 값]
            r.getTitle().trim(),
            r.getTarget().trim(),
            r.getStartDate(), r.getEndDate(),
            r.getBenefit().trim(),
            r.getCondition().trim(),
            type.name(),

            // [샘플 출력 양식] 치환 순서
            r.getTitle().trim(),                       // 제목
            school,                           // 총학생회 앞 학교명
            "",                                        // 총학생회 라인 뒤 추가 문구가 없다면 빈칸
            String.format("%s ~ %s", r.getStartDate(), r.getEndDate()),
            r.getTarget().trim(),
            r.getStartDate(), r.getEndDate(),
            r.getTarget().trim(),
            postImageLine
        );
    }

    // 이미지 업로드/변경
    private void handleImageUpload(Post entity, MultipartFile file) {
        if (file == null || file.isEmpty()) return;

        try {
            // 기존 이미지가 존재한다면 삭제
            if (entity.getPostImage() != null && !entity.getPostImage().isBlank()) {
                s3Service.deleteImageUrl(entity.getPostImage());
            }

            String uploadUrl = s3Service.upload(file);
            entity.setPostImage(uploadUrl);
        } catch (IOException e) {
            throw new GeneralException(ErrorStatus.INTERNAL_ERROR, "이미지 업로드에 실패했습니다.");
        }
    }

}
