package com.wasac.ne;

import com.wasac.ne.config.JwtProperties;
import com.wasac.ne.config.OtpProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, OtpProperties.class})
@EnableScheduling
public class NeApplication {

	public static void main(String[] args) {
		SpringApplication.run(NeApplication.class, args);
	}

}
