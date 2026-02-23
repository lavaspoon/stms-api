package devlava.stmsapi.service;

public class ReportTemplate {

    /**
     * HTML 뉴스클립 템플릿 구조 (v2 - Professional News Report)
     * CSS는 별도 파일(/static/news-clip.css)에 저장
     *
     * 사용 가능한 플레이스홀더:
     *   {{REPORT_TITLE}}    - 보고서 제목
     *   {{REPORT_SUBTITLE}} - 부제목 (보고 유형 등)
     *   {{REPORT_DATE}}     - 보고 기간/날짜
     *   {{REPORT_ORG}}      - 조직명 (예: 경영성과관리팀)
     *   {{TASK_TYPE_BADGE}} - 과제유형 뱃지 텍스트
     *   {{TOTAL_TASKS}}     - 총 과제 수
     *   {{INPUTTED_TASKS}}  - 활동 내역 입력 과제 수
     *   {{REPORT_CONTENT}}  - 보고서 본문 (섹션 모음)
     */
    public static final String HTML_NEWS_CLIP_TEMPLATE = """
        <!DOCTYPE html>
        <html lang="ko">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>{{REPORT_TITLE}} — 경영성과 보고서</title>
            <link rel="stylesheet" href="/news-clip.css">
        </head>
        <body>
            <div class="news-clip-container">

                <!-- ▶ 상단 헤더 -->
                <header class="news-header">
                    <div class="report-edition-bar">
                        <span class="report-edition-label">경영성과관리시스템 · PERFORMANCE REPORT</span>
                        <span class="report-edition-date">{{REPORT_DATE}}</span>
                    </div>
                    <div class="report-masthead">
                        <div class="report-org">BUSINESS PERFORMANCE MANAGEMENT</div>
                        <h1>{{REPORT_TITLE}}</h1>
                    </div>
                    <div class="report-meta-bar">
                        <span class="report-meta-item">
                            <span>과제 유형</span>
                            <strong>{{TASK_TYPE_BADGE}}</strong>
                        </span>
                        <div class="report-meta-divider"></div>
                        <span class="report-meta-item">
                            <span>총 과제 수</span>
                            <strong>{{TOTAL_TASKS}}개</strong>
                        </span>
                        <div class="report-meta-divider"></div>
                        <span class="report-meta-item">
                            <span>활동 입력</span>
                            <strong>{{INPUTTED_TASKS}}개</strong>
                        </span>
                        <div class="report-meta-divider"></div>
                        <span class="report-meta-item">
                            <span>보고 기간</span>
                            <strong>{{REPORT_DATE}}</strong>
                        </span>
                    </div>
                </header>

                <!-- ▶ 본문 -->
                <main class="news-content">
                    {{REPORT_CONTENT}}
                </main>

                <!-- ▶ 푸터 -->
                <footer class="report-footer">
                    <div class="report-footer-left">
                        <strong>경영성과관리시스템</strong><br>
                        본 보고서는 시스템에서 자동 생성된 문서입니다.
                    </div>
                    <div class="report-footer-right">
                        발행일 {{REPORT_DATE}}
                    </div>
                </footer>

            </div>
        </body>
        </html>
        """;

    /**
     * 섹션 빌더 헬퍼 — AI가 아래 구조를 참고하도록 프롬프트에 포함
     *
     * <div class="news-section">
     *   <div class="section-title" data-index="01">
     *     <span class="section-title-text">섹션 제목</span>
     *   </div>
     *   <div class="section-content">
     *     ...내용...
     *   </div>
     * </div>
     */
    public static final String SECTION_STRUCTURE_HINT = """
        <!-- 섹션 구조 예시 (반드시 이 구조 준수) -->
        <div class="news-section">
            <div class="section-title" data-index="01">
                <span class="section-title-text">개요</span>
            </div>
            <div class="section-content">
                <p>내용...</p>
            </div>
        </div>
        """;
}
