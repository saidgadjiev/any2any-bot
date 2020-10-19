package ru.gadjini.any2any.configuration;

import com.antkorwin.xsync.XSync;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class XSyncConfiguration {

    @Bean
    public XSync<Long> longXSync() {
        return new XSync<>();
    }
}
