package nl.anlizi.agri.pfsc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PfscApplication {

    public static void main(String[] args) {
        SpringApplication.run(PfscApplication.class, args);
    }

}
