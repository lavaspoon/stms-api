package devlava.stmsapi.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 실적 대비 목표 달성률을 한 곳에서 계산하는 유틸리티.
 * - 일반: 달성률 = 실적값 / 목표값 * 100
 * - 역계산: 달성률 = 목표값 / 실적값 * 100 (실적이 낮을수록 달성률이 높아짐, 예: 불량률·불손응대 비율 등)
 */
public final class AchievementRateCalculator {

    private static final int SCALE_DIVISION = 4;
    private static final int SCALE_RESULT = 2;

    private AchievementRateCalculator() {
    }

    /**
     * 목표값·실적값·역계산 여부로 달성률(%)을 계산한다.
     *
     * @param targetValue 목표값 (null이거나 0 이하면 0 반환)
     * @param actualValue 실적값 (null이면 0으로 간주)
     * @param reverseYn   역계산 여부 (true: 목표/실적*100, false: 실적/목표*100)
     * @return 소수점 둘째자리까지 반올림한 달성률, 계산 불가 시 0
     */
    public static BigDecimal calculate(BigDecimal targetValue, BigDecimal actualValue, boolean reverseYn) {
        BigDecimal target = targetValue != null ? targetValue : BigDecimal.ZERO;
        BigDecimal actual = actualValue != null ? actualValue : BigDecimal.ZERO;

        if (target.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        if (reverseYn) {
            if (actual.compareTo(BigDecimal.ZERO) <= 0) {
                return BigDecimal.ZERO;
            }
            return target
                    .divide(actual, SCALE_DIVISION, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(SCALE_RESULT, RoundingMode.HALF_UP);
        }

        return actual
                .divide(target, SCALE_DIVISION, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(SCALE_RESULT, RoundingMode.HALF_UP);
    }

    /**
     * 역계산 여부가 문자열("Y"/"N")인 경우의 오버로드.
     */
    public static BigDecimal calculate(BigDecimal targetValue, BigDecimal actualValue, String reverseYn) {
        return calculate(targetValue, actualValue, "Y".equals(reverseYn));
    }
}
