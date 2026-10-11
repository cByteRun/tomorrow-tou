package edu.tou.grades;

import edu.tou.shared.BusinessException;
import java.util.*;

public final class GradeRules {
  private GradeRules() {}

  public static void validate(int value, String type) {
    BusinessException.require(value >= 0 && value <= 100, "Оценка — целое число от 0 до 100");
    BusinessException.require(
        Set.of("Текущая", "Рубежная", "Итоговая").contains(type), "Некорректный вид оценки");
  }

  public static Double average(Collection<Grade> grades) {
    return grades.isEmpty()
        ? null
        : Math.round(grades.stream().mapToInt(g -> g.value).average().orElseThrow() * 10.0) / 10.0;
  }
}
