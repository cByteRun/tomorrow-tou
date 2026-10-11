package edu.tou.schedule;

import edu.tou.shared.BusinessException;
import java.util.*;

public final class ScheduleRules {
  private ScheduleRules() {}

  public static boolean overlaps(Lesson a, Lesson b) {
    return !"cancelled".equals(a.status)
        && !"cancelled".equals(b.status)
        && a.date.equals(b.date)
        && a.start.isBefore(b.end)
        && b.start.isBefore(a.end);
  }

  public static Optional<String> conflict(Lesson a, List<Lesson> rows) {
    return rows.stream()
        .filter(b -> !Objects.equals(a.id, b.id) && overlaps(a, b))
        .map(
            b ->
                a.teacher.id.equals(b.teacher.id)
                    ? "преподавателя"
                    : a.room.id.equals(b.room.id)
                        ? "аудитории"
                        : a.group.id.equals(b.group.id) ? "группы" : null)
        .filter(Objects::nonNull)
        .findFirst();
  }

  public static void validate(Lesson l, long groupSize, List<Lesson> existing) {
    BusinessException.require(
        l.start.isBefore(l.end), "Окончание занятия должно быть позже начала");
    BusinessException.require(
        "active".equals(l.semester.status), "Новые занятия допускаются только в активном семестре");
    BusinessException.require(
        !l.date.isBefore(l.semester.start) && !l.date.isAfter(l.semester.end),
        "Дата занятия вне границ семестра");
    BusinessException.require(
        l.teacher.subjects.stream().anyMatch(s -> s.id.equals(l.subject.id)),
        "Преподавателю не назначена эта дисциплина");
    BusinessException.require(
        l.group.subjects.stream().anyMatch(s -> s.id.equals(l.subject.id)),
        "Дисциплина отсутствует в учебном плане группы");
    BusinessException.require(
        groupSize <= l.room.capacity, "В аудитории недостаточно мест для группы");
    BusinessException.require(
        Set.of("scheduled", "conducted", "cancelled").contains(l.status),
        "Некорректный статус занятия");
    conflict(l, existing)
        .ifPresent(
            s -> {
              throw new BusinessException(409, "Конфликт расписания " + s);
            });
  }
}
