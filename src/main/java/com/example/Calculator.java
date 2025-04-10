package com.example;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Calculator {

    public int add(int a, int b) {
        log.info("Adding " + a + " and " + b);
        return a + b;
    }

    public int divide(int a, int b) {
        if (b == 0) throw new IllegalArgumentException("0으로 나눌 수 없습니다.");
        return a / b;
    }
}
