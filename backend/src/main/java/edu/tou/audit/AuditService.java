package edu.tou.audit;

import java.time.Instant;
import org.apache.logging.log4j.*;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
  private final AuditEventRepository repository;

  public AuditService(AuditEventRepository repository) {
    this.repository = repository;
  }

  public void record(String actor, String action, String entity, String description) {
    AuditEvent a = new AuditEvent();
    a.at = Instant.now();
    a.actor = actor;
    a.action = action;
    a.entity = entity;
    a.description = description;
    repository.save(a);
    LogManager.getLogger("AUDIT")
        .info(
            "actor={} action={} entity={} detail={}",
            actor,
            action,
            entity,
            description.replace('\n', ' ').replace('\r', ' '));
  }
}
