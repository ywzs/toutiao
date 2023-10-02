package com.heima.freemark;

import com.heima.freemarker;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@SpringBootTest(classes = freemarker.class)
@RunWith(SpringRunner.class)
public class produce {
    @Autowired
    Configuration configuration;

    @Test
    public void test() throws IOException {
        Template template = configuration.getTemplate("02-list.ftl");
    }
}
