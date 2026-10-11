package edu.tou.shared;

import static org.junit.jupiter.api.Assertions.*;

import edu.tou.identity.AccessSession;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.junit.jupiter.api.Test;

class ModelMappingTest {
  @Test
  void allThirteenEntitiesHaveConsistentHibernateMappings() {
    var registry =
        new StandardServiceRegistryBuilder()
            .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
            .applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
            .build();
    try {
      var sources = new MetadataSources(registry);
      EntityCatalog.TYPES.values().forEach(sources::addAnnotatedClass);
      sources.addAnnotatedClass(AccessSession.class);
      var metadata = (MetadataImplementor) sources.buildMetadata();
      assertDoesNotThrow(metadata::validate);
      assertEquals(13, metadata.getEntityBindings().size());
    } finally {
      StandardServiceRegistryBuilder.destroy(registry);
    }
  }
}
