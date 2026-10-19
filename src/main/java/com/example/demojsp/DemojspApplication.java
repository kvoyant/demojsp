package com.example.demojsp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan({"com.example.demojsp.domain.**.mapper", "com.example.demojsp.common.**.mapper"})
public class DemojspApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemojspApplication.class, args);
	}

}
