package edu.tou.welcome;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AttendanceNarrativeTest {
  @Test
  void teacherChecksHappenOnceThenEveryStudentPassesIdentityProximityAndJournal() {
    AttendanceNarrative story = new AttendanceNarrative();
    for (int teacher = 0; teacher < 3; teacher++) {
      assertEquals(teacher, story.phase());
      assertEquals(0, story.completed());
      story.advance();
    }
    for (int student = 0; student < 6; student++) {
      for (int phase = 3; phase <= 5; phase++) {
        assertEquals(student, story.student());
        assertEquals(phase, story.phase());
        assertEquals(student + (phase == 5 ? 1 : 0), story.completed());
        story.advance();
      }
    }
    assertEquals(0, story.phase());
    assertEquals(0, story.student());
    assertEquals(0, story.completed());
  }

  @Test
  void reducedMotionShowsAllStudentsAlreadyConfirmed() {
    AttendanceNarrative story = new AttendanceNarrative();
    story.complete();
    assertEquals(6, story.completed());
    assertEquals(5, story.phase());
  }
}
