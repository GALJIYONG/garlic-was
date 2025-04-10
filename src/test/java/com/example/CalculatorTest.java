package com.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CalculatorTest {

    @Test
    void testAdd() {
        Calculator calc = new Calculator();
        assertEquals(5, calc.add(2, 3));
    }

    @Test
    void testDivide() {
        Calculator calc = new Calculator();
        assertEquals(2, calc.divide(6, 3));
    }

    @Test
    void testDivideByZero() {
        Calculator calc = new Calculator();
        Exception ex = assertThrows(IllegalArgumentException.class, () -> calc.divide(1, 0));
        assertEquals("0으로 나눌 수 없습니다.", ex.getMessage());
    }
}
