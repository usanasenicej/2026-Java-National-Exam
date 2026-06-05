package com.wasac.ne;

import com.wasac.ne.config.JwtProperties;
import com.wasac.ne.config.OtpProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, OtpProperties.class})
public class NeApplication {

	public static void main(String[] args) {
		SpringApplication.run(NeApplication.class, args);
	}

}
