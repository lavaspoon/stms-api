# 뉴스클립 디자인 수정 가이드

이 문서는 AI 보고서 생성 기능의 뉴스클립 스타일을 수정하고 재디자인하는 방법을 안내합니다.

## 📁 수정할 파일 위치

뉴스클립 스타일을 변경하려면 다음 파일들을 수정하면 됩니다:

### 1. CSS 파일 (스타일 정의)
**파일 경로:** `stms-api/src/main/resources/static/news-clip.css`

이 파일에서 뉴스클립의 모든 시각적 스타일을 정의합니다:
- 색상, 폰트, 크기
- 레이아웃, 여백, 정렬
- 배경, 테두리, 그림자 효과
- 반응형 디자인 (모바일)

**주요 CSS 클래스:**
- `.news-clip-container`: 전체 뉴스클립 컨테이너
- `.news-header`: 헤더 영역 (제목, 날짜)
- `.news-content`: 본문 영역
- `.news-section`: 섹션 구분
- `.section-title`: 섹션 제목
- `.section-content`: 섹션 본문
- `.highlight-card`: 강조 카드
- `.stats-grid`: 통계 그리드
- `.stat-card`: 통계 카드
- `.task-item`: 과제 항목

### 2. HTML 템플릿 파일 (구조 정의)
**파일 경로:** `stms-api/src/main/java/devlava/stmsapi/service/ReportTemplate.java`

이 파일에서 뉴스클립의 HTML 구조를 정의합니다:
- HTML 태그 구조
- CSS 클래스 이름
- 플레이스홀더 위치 ({{REPORT_TITLE}}, {{REPORT_DATE}}, {{REPORT_CONTENT}})

**변수명:** `HTML_NEWS_CLIP_TEMPLATE`

### 3. AI 프롬프트 설정 (데이터 채우기 지시)
**파일 경로:** `stms-api/src/main/java/devlava/stmsapi/service/AIService.java`

이 파일의 `generateHTMLReport` 메서드에서:
- HTML 구조 사용 방법 안내
- 사용 가능한 CSS 클래스 목록
- 데이터 채우기 지시사항

## 🎨 디자인 수정 방법

### 색상 변경
`news-clip.css` 파일에서 다음 항목을 수정:
- 배경색: `body` → `background`
- 헤더 색상: `.news-header` → `background`
- 텍스트 색상: `.section-content` → `color`
- 강조 색상: `.section-title` → `color`, `border-bottom`

### 폰트 변경
`news-clip.css` 파일에서:
- 본문 폰트: `body` → `font-family`
- 제목 폰트: `.news-header h1` → `font-family`
- 섹션 제목: `.section-title` → `font-family`

### 레이아웃 변경
- 컨테이너 너비: `.news-clip-container` → `max-width`
- 여백: `.news-content` → `padding`
- 섹션 간격: `.news-section` → `margin-bottom`

### 효과 추가/제거
- 그림자: `box-shadow` 속성 수정
- 테두리: `border` 속성 수정
- 배경 패턴: `background-image` 속성 추가/제거

## 📝 HTML 구조 수정

HTML 구조를 변경하려면 `ReportTemplate.java`의 `HTML_NEWS_CLIP_TEMPLATE` 상수를 수정:

```java
public static final String HTML_NEWS_CLIP_TEMPLATE = """
    <!DOCTYPE html>
    <html lang="ko">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>과제 보고서</title>
        <link rel="stylesheet" href="/news-clip.css">
    </head>
    <body>
        <div class="news-clip-container">
            <!-- 여기서 HTML 구조 수정 -->
        </div>
    </body>
    </html>
    """;
```

**주의사항:**
- CSS 링크 (`<link rel="stylesheet" href="/news-clip.css">`)는 반드시 유지
- 플레이스홀더 (`{{REPORT_TITLE}}`, `{{REPORT_DATE}}`, `{{REPORT_CONTENT}}`)는 AI가 데이터로 교체
- CSS 클래스명은 `news-clip.css`와 일치해야 함

## 🔧 CSS 클래스 추가/수정

새로운 스타일 클래스를 추가하려면:

1. `news-clip.css`에 새로운 클래스 정의
2. `AIService.java`의 프롬프트에 사용 가능한 클래스 목록 추가
3. AI가 해당 클래스를 사용할 수 있도록 지시

예시:
```css
.my-custom-class {
    /* 스타일 정의 */
}
```

그리고 `AIService.java`의 프롬프트에:
```
- .my-custom-class: 사용 목적 설명
```

## 🚀 수정 후 적용 방법

1. **CSS 파일 수정**: `news-clip.css` 수정 후 저장
2. **HTML 템플릿 수정**: `ReportTemplate.java` 수정 후 재컴파일
3. **프롬프트 수정**: `AIService.java` 수정 후 재컴파일
4. **서버 재시작**: Spring Boot 애플리케이션 재시작
5. **테스트**: AI 보고서 생성 기능으로 확인

## 💡 팁

- CSS만 수정하면 즉시 적용되므로, 색상/폰트/레이아웃 변경은 CSS만 수정
- HTML 구조 변경 시에는 `ReportTemplate.java`와 프롬프트도 함께 수정 필요
- 반응형 디자인은 `@media` 쿼리 섹션에서 수정
- 브라우저 개발자 도구로 실시간 스타일 테스트 권장

## 📌 파일 요약

| 파일 | 역할 | 수정 빈도 |
|------|------|----------|
| `news-clip.css` | 스타일 정의 | 자주 (색상, 레이아웃 변경) |
| `ReportTemplate.java` | HTML 구조 | 가끔 (구조 변경 시) |
| `AIService.java` | AI 프롬프트 | 가끔 (클래스 추가 시) |

---

**가장 자주 수정하는 파일:** `stms-api/src/main/resources/static/news-clip.css`
