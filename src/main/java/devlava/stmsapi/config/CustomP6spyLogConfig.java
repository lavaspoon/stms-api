package devlava.stmsapi.config;

import com.p6spy.engine.spy.P6SpyOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomP6spyLogConfig {

    @PostConstruct
    public void setLogMessageFormat() {
        P6SpyOptions.getActiveInstance().setLogMessageFormat(CustomP6spySqlFormatUtils.class.getName());
    }

}
