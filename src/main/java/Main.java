import util.ScrapUtil;
import util.Selenium;

public class Main {

    public static final int START_ROW = 373;
    public static final int END_ROW = 373;
    public static final int BUNDLE_SIZE = 60;

    public static void main(String[] args) {
        Scraper scraper = new Scraper();
        ExcelHelper helper = new ExcelHelper();

        try {
            scraper.start(START_ROW, END_ROW, BUNDLE_SIZE);
//            helper.integrateExcels(START_ROW, END_ROW, BUNDLE_SIZE);
//            helper.createCompareSet(START_ROW, END_ROW);

        } catch (Exception e) {
            e.printStackTrace();
        }

        Selenium.quit();
    }
}
