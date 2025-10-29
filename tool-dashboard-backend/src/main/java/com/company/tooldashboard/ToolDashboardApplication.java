package com.company.tooldashboard;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 工具看板系统启动类
 */
@SpringBootApplication
@MapperScan("com.company.tooldashboard.mapper")
public class ToolDashboardApplication {

    public static void main(String[] args) {
        SpringApplication.run(ToolDashboardApplication.class, args);
    }

}
