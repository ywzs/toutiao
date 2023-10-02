package com.heima.wemedia.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan({"com.heima.apis.article.fallback","com.heima.apis.schedule.fallback"})
public class InitConfig {
}
