package com.heima;

import com.heima.file.service.FileStorageService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@SpringBootTest(classes = minioApplication.class)
@RunWith(SpringRunner.class)
public class mini {
    @Autowired
    FileStorageService fileStorageService;
    @Test
    public void test() throws ServerException, InvalidBucketNameException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        //创建minio客户端
        MinioClient client = MinioClient.builder()
                .credentials("minio", "minio123")
                .endpoint("http://192.168.200.130:9000").build();
//        FileInputStream fileInputStream  = new FileInputStream("D:\\sky.sql");
//        FileInputStream fileInputStream  = new FileInputStream("D:\\java3\\hmtt\\plugins\\cdnindex.css");
//        FileInputStream fileInputStream  = new FileInputStream("D:\\java3\\hmtt\\plugins\\vant.min.js");
        FileInputStream fileInputStream  = new FileInputStream("D:\\java3\\hmtt\\plugins\\vue.min.js");
        PutObjectArgs args = PutObjectArgs.builder()
                .object("plugins/js/vue.min.js")
                .contentType("text/plain")
                .bucket("leadnews")
                .stream(fileInputStream,fileInputStream.available(),-1)
                .build();
        System.out.println("http://192.168.200.130:9000/leadnews/plugins/js/vue.min.js");
        client.putObject(args);

    }

    @Test
    public void test1() throws FileNotFoundException {
        FileInputStream fileInputStream  = new FileInputStream("D:\\java3\\hmtt\\plugins\\cdnindex.css");
//        FileInputStream fileInputStream  = new FileInputStream("D:\\java3\\hmtt\\plugins\\vant.min.js");
//        FileInputStream fileInputStream  = new FileInputStream("D:\\java3\\hmtt\\plugins\\vue.min.js");
        String url = fileStorageService.uploadHtmlFile("plugins/css", "cdnindex.css", fileInputStream);
        System.out.println(url);
    }

    @Test
    public void test2(){
        System.out.println("你好，，我讨厌乱码");
        System.out.println(System.getProperty("file.encoding"));
    }
}
