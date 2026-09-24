package com.seyyah;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

// Flyway V1..V4 gerçek bir PostGIS konteynerine uygulanır, sonra ddl-auto: validate entity/şema
// uyumunu doğrular. jwt.gizli ve ors.api.key bağlamın açılması için gerekli, gerçek değerler değil.
@SpringBootTest
@TestPropertySource(properties = {
		"jwt.gizli=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcd",
		"ors.api.key=test-anahtar"
})
class SeyyahApplicationTests extends PostgisTestDestegi {

	@Test
	void contextLoads() {
	}

}
