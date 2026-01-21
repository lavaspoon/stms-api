package devlava.stmsapi.service;

public class ReportTemplate {
    
    /**
     * HTML 뉴스클립 템플릿 구조
     * CSS는 별도 파일(/static/news-clip.css)에 저장되어 있음
     */
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
                <div class="news-header">
                    <h1>{{REPORT_TITLE}}</h1>
                    <div class="date">{{REPORT_DATE}}</div>
                </div>
                <div class="news-content">
                    {{REPORT_CONTENT}}
                </div>
            </div>
        </body>
        </html>
        """;
}
