package hr.tvz.experimate.experimate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ExperiMateApplication {

    private static final Logger log = LoggerFactory.getLogger(ExperiMateApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(ExperiMateApplication.class, args);
        log.info("==================Application started==================");
	}
}
