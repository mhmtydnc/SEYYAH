package com.seyyah;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * DB gerektiren tüm testler bu sınıftan türetilir. Konteyner static olduğundan JVM başına bir kez
 * ayağa kalkar ve alt sınıflar arasında paylaşılır. Docker yoksa (yerel geliştirme) testler atlanır,
 * CI'da (GitHub Actions ubuntu runner, Docker kurulu) normal çalışır.
 */
@Testcontainers(disabledWithoutDocker = true)
public abstract class PostgisTestDestegi {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGIS = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:18-3.6").asCompatibleSubstituteFor("postgres"));
}
