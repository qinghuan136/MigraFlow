package com.qinghuan.migraflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication(proxyBeanMethods = false)
public class Main {

    public static void main(String[] args) {
        if (args.length > 0) {
            System.err.println("请启动后输入本地目录，当前不支持启动参数。");
            System.exit(2);
        }

        SpringApplication application = new SpringApplication(Main.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        try (ConfigurableApplicationContext context = application.run(args)) {
            // CommandLineRunner 返回时交互已结束，随后关闭容器并释放资源。
        } catch (RuntimeException exception) {
            System.err.println("CLI 启动或输入处理失败：" + exception.getMessage());
            System.exit(1);
        }
    }
}
