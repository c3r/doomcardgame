package pl.c3r.doomcardgame;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages={"pl.c3r.doomcardgame"})
public class DCG
{
    public static void main(String[] args)
    {
        SpringApplication.run(DCG.class, args);
    }
}
