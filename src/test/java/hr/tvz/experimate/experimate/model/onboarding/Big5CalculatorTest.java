package hr.tvz.experimate.experimate.model.onboarding;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Big5CalculatorTest {
    Big5Calculator calculator = new Big5Calculator();

    @Test
    void minusOneReturnsZero() {
        int result = calculator.normalizeForDisplay(-1.0);
        assertEquals(0, result);
    }

    @Test
    void zeroReturnsFifty() {
        int result = calculator.normalizeForDisplay(0.0);
        assertEquals(50, result);
    }

    @Test
    void oneReturnsHundred() {
        int result = calculator.normalizeForDisplay(1.0);
        assertEquals(100, result);
    }
}