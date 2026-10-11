package edu.tou.dashboard;

import edu.tou.attendance.Attendance;
import edu.tou.curriculum.Subject;
import edu.tou.grades.*;
import edu.tou.schedule.Lesson;
import edu.tou.shared.*;
import edu.tou.university.UniversityService;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@Service
public class DashboardService {
  private final UniversityService service;

  public DashboardService(UniversityService s) {
    service = s;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> overview() {
    List<Grade> grades = service.visible("grades").stream().map(e -> (Grade) e).toList();
    List<Attendance> marks =
        service.visible("attendance").stream().map(e -> (Attendance) e).toList();
    List<Lesson> lessons = service.visible("lessons").stream().map(e -> (Lesson) e).toList();
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("students", service.visible("students").size());
    out.put("subjects", service.visible("subjects").size());
    out.put("teachers", service.visible("teachers").size());
    out.put("average", GradeRules.average(grades));
    out.put(
        "todayCount",
        lessons.stream()
            .filter(l -> l.date.equals(LocalDate.now()) && !l.status.equals("cancelled"))
            .count());
    out.put(
        "todayLessons",
        lessons.stream()
            .filter(l -> l.date.equals(LocalDate.now()))
            .sorted(Comparator.comparing(l -> l.start))
            .map(service::dto)
            .toList());
    out.put(
        "subjectChart",
        service.visible("subjects").stream()
            .map(
                e -> {
                  Subject s = (Subject) e;
                  Map<String, Object> m = new LinkedHashMap<>();
                  m.put("name", s.name);
                  m.put(
                      "average",
                      GradeRules.average(
                          grades.stream().filter(g -> g.lesson.subject.id.equals(s.id)).toList()));
                  return m;
                })
            .toList());
    Map<String, Long> attendance = new LinkedHashMap<>();
    for (String status : List.of("present", "late", "absent", "excused"))
      attendance.put(status, marks.stream().filter(a -> a.status.equals(status)).count());
    out.put("attendance", attendance);
    out.put(
        "attendancePercent",
        marks.isEmpty()
            ? null
            : Math.round(
                100.0 * (attendance.get("present") + attendance.get("late")) / marks.size()));
    out.put("reviews", service.visible("reviews").size());
    return out;
  }
}
