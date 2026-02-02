package devlava.stmsapi.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class ExternalDbConfig {

    /**
     * 외부 DB 서버 (150.4.137.76, SMS DB)용 DataSource
     * 익명 접속이므로 username, password 없이 설정
     */
    @Bean(name = "externalDataSource")
    public DataSource externalDataSource() {
        return DataSourceBuilder.create()
                .driverClassName("org.postgresql.Driver")
                .url("jdbc:postgresql://150.4.137.76:5432/SMS")
                // 익명 접속이므로 username, password 설정하지 않음
                .build();
    }

    /**
     * 외부 DB용 JdbcTemplate
     */
    @Bean(name = "externalJdbcTemplate")
    public JdbcTemplate externalJdbcTemplate(@Qualifier("externalDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
