package com.heima.xxl.job;

import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.stereotype.Component;

@Component
public class helloWord {
    @XxlJob("demoJobHandler")
    public void helloJob(){
        System.out.println("hello world");
    }
}
