package com.heima.sample.controller;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
public class helloController {
    @Resource
    KafkaTemplate<String,String> kafkaTemplate;
    @GetMapping("/hello")
    public void hello(){
        kafkaTemplate.send("hello","hello world 你好 kafka");
    }
}
