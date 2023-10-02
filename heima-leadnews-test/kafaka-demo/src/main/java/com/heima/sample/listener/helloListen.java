package com.heima.sample.listener;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class helloListen {
    @KafkaListener(topics = {"hello"})
    public void test(String message){
        if (!StringUtils.isEmpty(message)){
            System.out.println(message);
        }
    }
}
