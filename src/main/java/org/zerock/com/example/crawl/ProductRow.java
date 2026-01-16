package org.zerock.com.example.crawl;

public record ProductRow(
        String category,
        String pcode,
        String name,
        Integer price,
        String detailUrl,
        String imageUrl,
        String specText,
        String extraText,     // ✅ 추가
        String regMonth,
        Integer opinionCount,
        Double rating,
        Integer reviewCount
) {}
