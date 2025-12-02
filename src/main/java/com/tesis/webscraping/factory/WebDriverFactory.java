package com.tesis.webscraping.factory;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Component;

@Component
public class WebDriverFactory {

	private final ChromeOptions options;

    public WebDriverFactory(ChromeOptions options) {
        this.options = options;
    }

    public WebDriver createDriver() {
        return new ChromeDriver(options);
    }
}
