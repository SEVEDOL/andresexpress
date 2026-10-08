package com.andesexpress.notifications;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Sin el consumidor de SQS: la prueba no debe depender de una cola real
@SpringBootTest(properties = "aws.sqs.listener-enabled=false")
class NotificationsServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
