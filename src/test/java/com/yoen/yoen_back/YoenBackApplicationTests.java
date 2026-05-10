package com.yoen.yoen_back;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("Unit-test CI does not load the full Spring context until test infrastructure is configured.")
@SpringBootTest
class YoenBackApplicationTests {

	@Test
	void contextLoads() {
	}

}
