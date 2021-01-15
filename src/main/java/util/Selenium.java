package util;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Selenium {
    public static final String CLIENT_PATH = "/Users/kdpark/Documents/side_project/seojae/java_ver/src/main/resources/chromedriver";
    public static final String DOWNLOAD_PATH = "/Users/kdpark/Documents/side_project/seojae/java_ver/src/main/resources/pdf/";

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
        System.setProperty("webdriver.chrome.driver", CLIENT_PATH);

        ChromeOptions options = new ChromeOptions();
        Map<String, Object> chromePrefs = Map.of(
                "plugins.always_open_pdf_externally", true,
                "download.default_directory", DOWNLOAD_PATH
        );
        options.setExperimentalOption("prefs", chromePrefs);

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
