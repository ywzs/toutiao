package com.heima.sample;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class comsumer {
    public static void main(String[] args) {
        //1.连接配置
        Properties properties = new Properties();
        //kafka的连接地址
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "192.168.200.130:9092");
        //消费者组
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "group2");
        //关闭自动提交消息
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,false);
        //消息的反序列化器
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        //2.消费者对象
        KafkaConsumer<String,String> consumer = new KafkaConsumer<String, String>(properties);
        //3.订阅消息
        consumer.subscribe(Collections.singletonList("hello"));
        //4.拉取消息
        try {
            while (true){
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(3000));//3s拉取一次
                for (ConsumerRecord<String, String> record : records) {
                    System.out.println(record.key());
                    System.out.println(record.value());
                }
                consumer.commitAsync();
            }
        } catch (Exception e){
            System.out.println(e);
        }finally {
            consumer.commitSync();
        }

    }
}
