package com.seyyah;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * DB gerektiren tüm testler bu sınıftan türetilir. Docker yoksa (yerel geliştirme) testler atlanır,
 * CI'da (GitHub Actions ubuntu runner, Docker kurulu) normal çalışır.
 *
 * Konteyner JUnit'in @Container'ı ile değil Spring bean'i olarak yönetilir: @Container onu her test
 * sınıfının sonunda durdurur, ama önbellekteki Spring bağlamı sonraki sınıfta kapanmış porta bağlanmaya
 * çalışır. Bean olarak, önbellekteki bağlam yaşadıkça konteyner de açık kalır; tek örnek tüm bağlamlarca
 * paylaşılır, start() zaten çalışan konteynerde bir şey yapmaz.
 */
@Testcontainers(disabledWithoutDocker = true)
@Import(PostgisTestDestegi.Konteyner.class)
public abstract class PostgisTestDestegi {

    @TestConfiguration(proxyBeanMethods = false)
    static class Konteyner {

        private static final PostgreSQLContainer<?> POSTGIS = new PostgreSQLContainer<>(
                DockerImageName.parse("postgis/postgis:18-3.6").asCompatibleSubstituteFor("postgres"));

        // destroyMethod boş: bir bağlam kapanırken paylaşılan konteyneri durdurmasın (JVM sonunda Ryuk temizler)
        @Bean(destroyMethod = "")
        @ServiceConnection
        PostgreSQLContainer<?> postgis() {
            return POSTGIS;
        }
    }
}
