package devlava.stmsapi.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 달성률을 백엔드 한 곳에서 계산하는 유틸리티.
 * - 기준이 % (일반): 달성률(%) = 각 월 달성률의 합 / 월수
 * - 기준이 % (역계산): 월별 달성률 = 목표 ÷ 실적 × 100, 과제 달성률 = 각 월 달성률의 합 / 월수
 * - 기준이 건수·금액: 달성률(%) = 각 월 실적의 합 / 과제 목표 × 100 (역계산 시 목표/실적×100)
 * - 기준이 평균 목표(건수): 월별 달성률 = 실적 ÷ 월 목표(고정) × 100 → 과제 달성률 = 각 월 달성률의 합 ÷ 월 수
 */
public final class AchievementRateCalculator {

    private static final int SCALE_DIVISION = 4;
    private static final int SCALE_RESULT = 2;

    private AchievementRateCalculator() {
    }

    /**
     * 기준이 % (역계산) 단일 월: 월별 달성률 = 목표 ÷ 실적 × 100
     *
     * @param targetValue 목표값 (null이거나 0 이하면 0 반환)
     * @param actualValue 해당 월 실적 (null이거나 0 이하면 0 반환)
     * @return 소수점 둘째자리까지 반올림
     */
    public static BigDecimal calculatePercentReverseSingleMonth(BigDecimal targetValue, BigDecimal actualValue) {
        BigDecimal target = targetValue != null ? targetValue : BigDecimal.ZERO;
        BigDecimal actual = actualValue != null ? actualValue : BigDecimal.ZERO;
        if (target.compareTo(BigDecimal.ZERO) <= 0 || actual.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return target
                .divide(actual, SCALE_DIVISION, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(SCALE_RESULT, RoundingMode.HALF_UP);
    }

    /**
     * 기준이 % (역계산): 월별 달성률 = 목표 ÷ 실적 × 100, 과제 달성률 = 각 월 달성률의 합 / 월수
     *
     * @param targetValue    과제 목표값 (예: 2.1%)
     * @param monthlyActuals 각 월 실적 목록 (예: 2.1, 1.5, 1.5)
     * @return 소수점 둘째자리까지 반올림, 목록이 비어 있으면 0
     */
    public static BigDecimal calculatePercentReverseFromMonthlyActuals(BigDecimal targetValue,
            List<BigDecimal> monthlyActuals) {
        if (targetValue == null || targetValue.compareTo(BigDecimal.ZERO) <= 0 || monthlyActuals == null
                || monthlyActuals.isEmpty()) {
            return BigDecimal.ZERO;
        }
        List<BigDecimal> monthlyRates = monthlyActuals.stream()
                .filter(v -> v != null)
                .map(actual -> calculatePercentReverseSingleMonth(targetValue, actual))
                .collect(Collectors.toList());
        if (monthlyRates.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return calculatePercentFromMonthlyRates(monthlyRates);
    }

    /**
     * 기준이 % (일반 방향): 월별 달성률(%) = 실적 ÷ 목표 × 100
     * 과제 달성률(%) = 각 월 달성률의 합 / 월 수
     *
     * @param targetValue    과제 목표값
     * @param monthlyActuals 각 월 실적 목록
     * @return 소수점 둘째자리까지 반올림, 목록이 비어 있으면 0
     */
    public static BigDecimal calculatePercentFromMonthlyActuals(BigDecimal targetValue,
            List<BigDecimal> monthlyActuals) {
        if (targetValue == null || targetValue.compareTo(BigDecimal.ZERO) <= 0 || monthlyActuals == null
                || monthlyActuals.isEmpty()) {
            return BigDecimal.ZERO;
        }
        List<BigDecimal> monthlyRates = monthlyActuals.stream()
                .filter(v -> v != null)
                .map(actual -> calculate(targetValue, actual, false))
                .collect(Collectors.toList());
        if (monthlyRates.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return calculatePercentFromMonthlyRates(monthlyRates);
    }

    /**
     * 기준이 % (일반 방향): 달성률(%) = 각 월 달성률의 합 / 월수
     *
     * @param monthlyRates 각 월의 달성률(%) 목록 (TbTaskActivity.actualValue가 월별 %인 경우)
     * @return 소수점 둘째자리까지 반올림, 목록이 비어 있으면 0
     */
    public static BigDecimal calculatePercentFromMonthlyRates(List<BigDecimal> monthlyRates) {
        if (monthlyRates == null || monthlyRates.isEmpty()) {
            return BigDecimal.ZERO;
        }
        List<BigDecimal> valid = monthlyRates.stream()
                .filter(r -> r != null)
                .collect(Collectors.toList());
        if (valid.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = valid.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum
                .divide(BigDecimal.valueOf(valid.size()), SCALE_DIVISION, RoundingMode.HALF_UP)
                .setScale(SCALE_RESULT, RoundingMode.HALF_UP);
    }

    /**
     * 기준이 건수·금액인 경우: 달성률(%) = 각 월 실적의 합 / 과제 목표 * 100 (역계산 시 목표/실적*100)
     *
     * @param targetValue      과제 목표값 (null이거나 0 이하면 0 반환)
     * @param sumOfActualValue 각 월 실적의 합 (null이면 0으로 간주)
     * @param reverseYn        역계산 여부 (true: 목표/실적*100)
     * @return 소수점 둘째자리까지 반올림한 달성률
     */
    public static BigDecimal calculateFromSum(BigDecimal targetValue, BigDecimal sumOfActualValue, boolean reverseYn) {
        BigDecimal target = targetValue != null ? targetValue : BigDecimal.ZERO;
        BigDecimal sum = sumOfActualValue != null ? sumOfActualValue : BigDecimal.ZERO;

        if (target.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        if (reverseYn) {
            if (sum.compareTo(BigDecimal.ZERO) <= 0) {
                return BigDecimal.ZERO;
            }
            return target
                    .divide(sum, SCALE_DIVISION, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(SCALE_RESULT, RoundingMode.HALF_UP);
        }

        return sum
                .divide(target, SCALE_DIVISION, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(SCALE_RESULT, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateFromSum(BigDecimal targetValue, BigDecimal sumOfActualValue, String reverseYn) {
        return calculateFromSum(targetValue, sumOfActualValue, "Y".equals(reverseYn));
    }

    /**
     * 목표값·실적값·역계산 여부로 달성률(%)을 계산한다. (단일 실적값용, 예: 월별 활동 1건)
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

    public static BigDecimal calculate(BigDecimal targetValue, BigDecimal actualValue, String reverseYn) {
        return calculate(targetValue, actualValue, "Y".equals(reverseYn));
    }

    /**
     * 기준이 평균 목표(건수): 월별 달성률 = 실적 ÷ 월 목표(고정) × 100, 과제 달성률 = 각 월 달성률의 합 ÷ 월 수
     *
     * @param monthlyTarget  월 목표 건수 (고정, null이거나 0 이하면 0 반환)
     * @param monthlyActuals 각 월 실적 건수 목록
     * @return 소수점 둘째자리까지 반올림, 목록이 비어 있으면 0
     */
    public static BigDecimal calculateMonthlyAvgCountFromActuals(BigDecimal monthlyTarget,
            List<BigDecimal> monthlyActuals) {
        if (monthlyTarget == null || monthlyTarget.compareTo(BigDecimal.ZERO) <= 0 || monthlyActuals == null
                || monthlyActuals.isEmpty()) {
            return BigDecimal.ZERO;
        }
        List<BigDecimal> monthlyRates = monthlyActuals.stream()
                .filter(v -> v != null)
                .map(actual -> calculate(monthlyTarget, actual, false))
                .collect(Collectors.toList());
        if (monthlyRates.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return calculatePercentFromMonthlyRates(monthlyRates);
    }
}
