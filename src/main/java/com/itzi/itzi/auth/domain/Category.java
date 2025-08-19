package com.itzi.itzi.auth.domain;

public enum Category {
    FOOD("음식점/카페"),
    FASHION("의류/패션"),
    BEAUTY("뷰티/미용"),
    HEALTH("헬스/피트니스"),
    BOOK("문구/서점"),
    LIVING("생활/잡화"),
    HOSPITAL("병원/약국"),
    IT("전자/IT"),
    TRANSPORTATION("교통/이동"),
    ETC("기타");  // 기타

    private final String description;

    // 생성자
    Category(String description) {
        this.description = description;
    }

    // getter
    public String getDescription() {
        return description;
    }
}