package com.resumade.template;

import com.resumade.template.entity.Template;
import com.resumade.template.repository.TemplateRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableDiscoveryClient
public class TemplateServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TemplateServiceApplication.class, args);
    }

    @Bean
    public CommandLineRunner loadData(TemplateRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                Template pro = new Template("Professional Classic", "Clean and ATS friendly", "assets/templates/pro.png", "<div class='pro'>{{content}}</div>", ".pro { font-family: Arial; }", Template.Category.PROFESSIONAL, false);
                Template creative = new Template("Creative Studio", "Stand out from the crowd", "assets/templates/creative.png", "<div class='creative'>{{content}}</div>", ".creative { font-family: Inter; }", Template.Category.CREATIVE, true);
                Template modern = new Template("Modern Tech", "For modern tech roles", "assets/templates/modern.png", "<div class='modern'>{{content}}</div>", ".modern { font-family: Roboto; }", Template.Category.MODERN, false);
                
                pro.setUsageCount(150);
                creative.setUsageCount(320);
                modern.setUsageCount(980);
                
                repository.save(pro);
                repository.save(creative);
                repository.save(modern);
                System.out.println("Seeded initial templates");
            }
        };
    }
}
