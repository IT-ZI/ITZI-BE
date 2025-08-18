package com.itzi.itzi.posts.domain;

// 홈에서 게시물 필터링 기준
public enum OrderBy {
    CLOSING,            // exposureEndDate
    POPULAR,             // bookmarkCount
    LATEST,             // publishedAt DESC
    OLDEST              // publishedAt ASC

}
