package edu.tou.schedule;

import static edu.tou.shared.Fixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import edu.tou.shared.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;

class ScheduleRulesTest {
  @Test
  void teacherOverlapIsRejected() {
    Lesson a = lesson(1), b = lesson(2);
    b.room.id = 2L;
    b.group.id = 2L;
    assertEquals("преподавателя", ScheduleRules.conflict(a, List.of(b)).orElseThrow());
  }

  @Test
  void roomOverlapIsRejected() {
    Lesson a = lesson(1), b = lesson(2);
    b.teacher.id = 2L;
    b.group.id = 2L;
    assertEquals("аудитории", ScheduleRules.conflict(a, List.of(b)).orElseThrow());
  }

  @Test
  void groupOverlapIsRejected() {
    Lesson a = lesson(1), b = lesson(2);
    b.teacher.id = 2L;
    b.room.id = 2L;
    assertEquals("группы", ScheduleRules.conflict(a, List.of(b)).orElseThrow());
  }

  @Test
  void touchingBoundariesAreAllowed() {
    Lesson a = lesson(1), b = lesson(2);
    b.start = a.end;
    b.end = b.start.plusHours(1);
    assertFalse(ScheduleRules.overlaps(a, b));
  }

  @Test
  void differentDatesAreAllowed() {
    Lesson a = lesson(1), b = lesson(2);
    b.date = b.date.plusDays(1);
    assertTrue(ScheduleRules.conflict(a, List.of(b)).isEmpty());
  }

  @Test
  void unrelatedParallelLessonsAreAllowed() {
    Lesson a = lesson(1), b = lesson(2);
    b.teacher.id = 2L;
    b.room.id = 2L;
    b.group.id = 2L;
    assertTrue(ScheduleRules.conflict(a, List.of(b)).isEmpty());
  }

  @Test
  void currentLessonDoesNotConflictWithItself() {
    Lesson a = lesson(1);
    assertTrue(ScheduleRules.conflict(a, List.of(a)).isEmpty());
  }

  @Test
  void cancelledLessonDoesNotOccupyResource() {
    Lesson a = lesson(1), b = lesson(2);
    b.status = "cancelled";
    assertTrue(ScheduleRules.conflict(a, List.of(b)).isEmpty());
  }

  @Test
  void cancelledCandidateDoesNotOccupyResource() {
    Lesson a = lesson(1), b = lesson(2);
    a.status = "cancelled";
    assertTrue(ScheduleRules.conflict(a, List.of(b)).isEmpty());
  }

  @Test
  void nestedIntervalsOverlap() {
    Lesson a = lesson(1), b = lesson(2);
    b.start = a.start.plusMinutes(15);
    b.end = a.end.minusMinutes(15);
    assertTrue(ScheduleRules.overlaps(a, b));
  }

  @Test
  void zeroDurationIsRejected() {
    Lesson a = lesson(1);
    a.end = a.start;
    assertThrows(BusinessException.class, () -> ScheduleRules.validate(a, 10, List.of()));
  }

  @Test
  void negativeDurationIsRejected() {
    Lesson a = lesson(1);
    a.end = a.start.minusMinutes(1);
    assertThrows(BusinessException.class, () -> ScheduleRules.validate(a, 10, List.of()));
  }

  @Test
  void archivedSemesterIsRejected() {
    Lesson a = lesson(1);
    a.semester.status = "archived";
    assertThrows(BusinessException.class, () -> ScheduleRules.validate(a, 10, List.of()));
  }

  @Test
  void dateBeforeSemesterIsRejected() {
    Lesson a = lesson(1);
    a.date = a.semester.start.minusDays(1);
    assertThrows(BusinessException.class, () -> ScheduleRules.validate(a, 10, List.of()));
  }

  @Test
  void dateAfterSemesterIsRejected() {
    Lesson a = lesson(1);
    a.date = a.semester.end.plusDays(1);
    assertThrows(BusinessException.class, () -> ScheduleRules.validate(a, 10, List.of()));
  }

  @Test
  void firstDayOfSemesterIsAllowed() {
    Lesson a = lesson(1);
    a.date = a.semester.start;
    assertDoesNotThrow(() -> ScheduleRules.validate(a, 10, List.of()));
  }

  @Test
  void lastDayOfSemesterIsAllowed() {
    Lesson a = lesson(1);
    a.date = a.semester.end;
    assertDoesNotThrow(() -> ScheduleRules.validate(a, 10, List.of()));
  }

  @Test
  void insufficientRoomIsRejected() {
    Lesson a = lesson(1);
    assertThrows(BusinessException.class, () -> ScheduleRules.validate(a, 21, List.of()));
  }

  @Test
  void exactRoomCapacityIsAllowed() {
    Lesson a = lesson(1);
    assertDoesNotThrow(() -> ScheduleRules.validate(a, 20, List.of()));
  }

  @Test
  void unassignedTeacherSubjectIsRejected() {
    Lesson a = lesson(1);
    a.teacher.subjects.clear();
    assertThrows(BusinessException.class, () -> ScheduleRules.validate(a, 10, List.of()));
  }

  @Test
  void missingCurriculumSubjectIsRejected() {
    Lesson a = lesson(1);
    a.group.subjects.clear();
    assertThrows(BusinessException.class, () -> ScheduleRules.validate(a, 10, List.of()));
  }

  @Test
  void unknownLessonStatusIsRejected() {
    Lesson a = lesson(1);
    a.status = "broken";
    assertThrows(BusinessException.class, () -> ScheduleRules.validate(a, 10, List.of()));
  }
}
