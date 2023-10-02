package com.heima.sample;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

public class producer {
    public static void main(String[] args) {
        //1.连接配置
        Properties properties = new Properties();
        //kafka的连接地址
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,"192.168.200.130:9092");
        //发送失败，失败的重试次数
        properties.put(ProducerConfig.RETRIES_CONFIG,5);
        //消息key的序列化器
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringSerializer");
        //消息value的序列化器
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringSerializer");
        //ack默认只需要leader节确认就认为发送成功
        properties.put(ProducerConfig.ACKS_CONFIG,"1");
        //压缩消息，节省网络传输开销 (snappy 和 lz4 节省cpu 性能好  。 gzip 压缩得更小)
        properties.put(ProducerConfig.COMPRESSION_TYPE_CONFIG,"snappy");
        //2.生产者对象
        KafkaProducer<String,String> kafkaProducer = new KafkaProducer<String, String>(properties);
        //3.接受消息
        //topic  key value
        ProducerRecord<String,String> producerRecord = new ProducerRecord<String,String>("hello","kafka","hello world");
        kafkaProducer.send(producerRecord);
        //4.关闭通道
        kafkaProducer.close();
    }
}
