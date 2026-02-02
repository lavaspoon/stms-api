package devlava.stmsapi.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ExternalDbService {

    private final JdbcTemplate externalJdbcTemplate;

    public ExternalDbService(@Qualifier("externalJdbcTemplate") JdbcTemplate externalJdbcTemplate) {
        this.externalJdbcTemplate = externalJdbcTemplate;
    }

    /**
     * EN_TRAN 테이블에 데이터 INSERT
     * 
     * @param dataMap INSERT할 컬럼명과 값의 Map
     * @return 영향받은 행 수
     */
    public int insertToEnTran(Map<String, Object> dataMap) {
        if (dataMap == null || dataMap.isEmpty()) {
            throw new IllegalArgumentException("INSERT할 데이터가 없습니다.");
        }

        // 컬럼명과 값 분리
        StringBuilder columns = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();
        
        for (String column : dataMap.keySet()) {
            if (columns.length() > 0) {
                columns.append(", ");
                placeholders.append(", ");
            }
            columns.append(column);
            placeholders.append("?");
        }

        String sql = String.format("INSERT INTO EN_TRAN (%s) VALUES (%s)", 
                                   columns.toString(), 
                                   placeholders.toString());
        
        if (sql == null) {
            throw new IllegalStateException("SQL 쿼리 생성 실패");
        }

        // 값 배열 생성
        Object[] values = dataMap.values().toArray();

        return externalJdbcTemplate.update(sql, values);
    }

    /**
     * 네이티브 쿼리 직접 실행
     * 
     * @param sql 실행할 SQL 쿼리
     * @param args 쿼리 파라미터
     * @return 영향받은 행 수
     */
    public int executeNativeQuery(String sql, Object... args) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL 쿼리가 비어있습니다.");
        }
        return externalJdbcTemplate.update(sql, args);
    }
}
