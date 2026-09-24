package com.seyyah;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * DB gerektiren tüm testler bu sınıftan türetilir. Docker yoksa (yerel geliştirme) testler atlanır,
 * CI'da (GitHub Actions ubuntu runner, Docker kurulu) normal çalışır.
 *
 * Konteyner JVM başına bir kez başlatılır ve Spring'e yalnızca bağlantı bilgisi verilir; yaşam döngüsü Spring'e
 * bağlı değildir. Önceki iki çözüm de kırılgandı: @Container onu her test sınıfının sonunda durduruyordu, bean
 * olarak yönetilince ise bağlamı açılamayan tek bir test kapanırken paylaşılan konteyneri durdurup diğer tüm DB
 * testlerini zaman aşımına düşürdü. Konteyneri JVM sonunda Testcontainers (Ryuk) temizler.
 */
@Testcontainers(disabledWithoutDocker = true)
public abstract class PostgisTestDestegi {

    static final PostgreSQLContainer<?> POSTGIS = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:18-3.6").asCompatibleSubstituteFor("postgres"));

    static {
        // Sınıf Docker'sız ortamda da yüklenir (JUnit sonra atlar); orada başlatmaya çalışma
        if (DockerClientFactory.instance().isDockerAvailable()) {
            POSTGIS.start();
        }
    }

    @DynamicPropertySource
    static void veritabani(DynamicPropertyRegistry kayit) {
        kayit.add("spring.datasource.url", POSTGIS::getJdbcUrl);
        kayit.add("spring.datasource.username", POSTGIS::getUsername);
        kayit.add("spring.datasource.password", POSTGIS::getPassword);
    }
}
