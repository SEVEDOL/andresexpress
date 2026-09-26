package com.andesexpress.notifications;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "aws.dynamodb.create-tables=false")
class NotificationsServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
