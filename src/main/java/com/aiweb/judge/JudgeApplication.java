package com.aiweb.judge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class JudgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(JudgeApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        System.out.println();
        System.out.println("===========================================");
        System.out.println("추상적 판사님 준비 완료! 접속해보셈");
        System.out.println(">>>> http://localhost:8080");
        System.out.println("===========================================");
        System.out.println();
    }

}
