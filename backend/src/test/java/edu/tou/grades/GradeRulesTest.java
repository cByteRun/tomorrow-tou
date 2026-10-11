package edu.tou.grades;

import static org.junit.jupiter.api.Assertions.*;

import edu.tou.shared.BusinessException;
import java.util.*;
import org.junit.jupiter.api.*;

class GradeRulesTest {
  private Grade grade(int value) {
    Grade g = new Grade();
    g.value = value;
    return g;
  }

  @Test
  void zeroScoreIsValid() {
    assertDoesNotThrow(() -> GradeRules.validate(0, "Текущая"));
  }

  @Test
  void hundredScoreIsValid() {
    assertDoesNotThrow(() -> GradeRules.validate(100, "Итоговая"));
  }

  @Test
  void negativeScoreIsRejected() {
    assertThrows(BusinessException.class, () -> GradeRules.validate(-1, "Текущая"));
  }

  @Test
  void excessiveScoreIsRejected() {
    assertThrows(BusinessException.class, () -> GradeRules.validate(101, "Текущая"));
  }

  @Test
  void invalidAssessmentTypeIsRejected() {
    assertThrows(BusinessException.class, () -> GradeRules.validate(80, "Произвольная"));
  }

  @Test
  void midtermTypeIsValid() {
    assertDoesNotThrow(() -> GradeRules.validate(80, "Рубежная"));
  }

  @Test
  void noGradesHasNoInventedAverage() {
    assertNull(GradeRules.average(List.of()));
  }

  @Test
  void averagesActualValues() {
    assertEquals(80.0, GradeRules.average(List.of(grade(60), grade(100))));
  }

  @Test
  void roundsToOneDecimal() {
    assertEquals(66.7, GradeRules.average(List.of(grade(0), grade(100), grade(100))));
  }

  @Test
  void includesZeroInAverage() {
    assertEquals(50.0, GradeRules.average(List.of(grade(0), grade(100))));
  }

  @Test
  void singleValueIsItsOwnAverage() {
    assertEquals(73.0, GradeRules.average(List.of(grade(73))));
  }
}
