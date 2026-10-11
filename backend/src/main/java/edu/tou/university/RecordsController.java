package edu.tou.university;

import edu.tou.shared.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/records")
public class RecordsController {
  private final UniversityService service;

  public RecordsController(UniversityService s) {
    service = s;
  }

  @GetMapping("/{kind}")
  public List<Map<String, Object>> list(@PathVariable String kind) {
    return service.list(kind);
  }

  @GetMapping("/{kind}/{id}")
  public Map<String, Object> get(@PathVariable String kind, @PathVariable long id) {
    return service.get(kind, id);
  }

  @PostMapping("/{kind}")
  public Map<String, Object> create(@PathVariable String kind, @RequestBody Map<String, Object> b) {
    return service.save(kind, null, b);
  }

  @PutMapping("/{kind}/{id}")
  public Map<String, Object> update(
      @PathVariable String kind, @PathVariable long id, @RequestBody Map<String, Object> b) {
    return service.save(kind, id, b);
  }

  @DeleteMapping("/{kind}/{id}")
  public void delete(@PathVariable String kind, @PathVariable long id, @RequestParam long version) {
    service.delete(kind, id, version);
  }

  @PostMapping("/attendance/mark-all/{id}")
  public void markAll(@PathVariable long id) {
    service.markAll(id);
  }
}
