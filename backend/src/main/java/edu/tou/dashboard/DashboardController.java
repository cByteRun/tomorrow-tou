package edu.tou.dashboard;

import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
public class DashboardController {
  private final DashboardService dashboard;

  public DashboardController(DashboardService d) {
    dashboard = d;
  }

  @GetMapping("/api/dashboard")
  public Map<String, Object> get() {
    return dashboard.overview();
  }
}
