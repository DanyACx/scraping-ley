package com.tesis.webscraping.config;

import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class WebDriverConfig {

	@Value("${webdriver.chrome.driver}")
    private String chromeDriverPath;

    @PostConstruct
    public void setupChromeDriver() {
        System.setProperty("webdriver.chrome.driver", chromeDriverPath);
    }
    
    @Bean
    public ChromeOptions chromeOptions() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-pdf-viewer");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        return options;
    }
}
