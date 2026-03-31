package com.datemate.domain.course.dto.response;

import com.datemate.domain.course.entity.CourseShare;
import java.time.LocalDateTime;

/**
 * 코스 공유 응답 DTO
 * 1. 공유 링크 생성 결과를 반환한다
 * 2. 프론트엔드는 shareUrl을 카카오톡 공유 등에 사용한다
 *
 * @param shareToken 공유용 고유 토큰
 * @param shareUrl 공유 딥링크 전체 URL
 * @param expiresAt 공유 링크 만료 시각
 */
public record CourseShareResponse(
    String shareToken,
    String shareUrl,
    LocalDateTime expiresAt
) {

    /**
     * 1. CourseShare 엔티티와 base URL로 응답 DTO를 생성한다
     * 2. shareUrl은 "datemate://course/{token}" 형식의 딥링크이다
     */
    public static CourseShareResponse of(CourseShare courseShare, String baseUrl) {
        String shareUrl = baseUrl + "/course/share/" + courseShare.getShareToken();
        return new CourseShareResponse(
            courseShare.getShareToken(),
            shareUrl,
            courseShare.getExpiresAt()
        );
    }
}
