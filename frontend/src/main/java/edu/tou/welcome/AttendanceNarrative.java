package edu.tou.welcome;

/** Visual narrative only; it never records attendance or accesses device sensors. */
public final class AttendanceNarrative {
  private int phase, student;

  public int phase() {
    return phase;
  }

  public int student() {
    return student;
  }

  public int completed() {
    return student + (phase == 5 ? 1 : 0);
  }

  public void advance() {
    if (++phase > 5) {
      if (++student == 6) {
        student = 0;
        phase = 0;
      } else phase = 3;
    }
  }

  public void complete() {
    student = 5;
    phase = 5;
  }
}
