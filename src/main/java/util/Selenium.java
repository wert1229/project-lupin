package util;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.concurrent.TimeUnit;

public class Selenium {

    private static ChromeDriver driver;

    public static ChromeDriver getInstance() {
        if (driver == null) {
            System.setProperty("webdriver.chrome.driver", "/Users/kdpark/Documents/side_project/seojae/java_ver/src/main/resources/chromedriver");
            driver = new ChromeDriver();
            driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);
        }

        return driver;
    }

    public static ChromeDriver newInstance() {
        System.setProperty("webdriver.chrome.driver", "/Users/kdpark/Documents/side_project/seojae/java_ver/src/main/resources/chromedriver");
        ChromeOptions options = new ChromeOptions();
        ChromeDriver driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        driver.manage().timeouts().pageLoadTimeout(400, TimeUnit.SECONDS).implicitlyWait(2, TimeUnit.SECONDS);
        return driver;
    }

    public static void quit() {
        if (driver != null) {
            driver.quit();
        }
    }
}
