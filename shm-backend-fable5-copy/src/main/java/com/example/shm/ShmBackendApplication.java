package com.example.shm;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan({
        "com.example.shm.vendorsource.mapper"
})
public class ShmBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShmBackendApplication.class, args);
    }

}
