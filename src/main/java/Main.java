import util.Selenium;

public class Main {
    public static void main(String[] args) {
        Scraper scraper = new Scraper();
        try {
            scraper.start(903, 1082, 30);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Selenium.quit();
    }
}
