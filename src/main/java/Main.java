import util.ExcelHelper;
import util.Scraper;

public class Main {

    public static final int START_ROW = 5;
    public static final int END_ROW = 724;
    public static final int BUNDLE_SIZE = 180;

    public static void main(String[] args) {
        Scraper scraper = new Scraper();
        ExcelHelper excelHelper = new ExcelHelper();

        try {
            scraper.start(START_ROW, END_ROW, BUNDLE_SIZE);
//            excelHelper.integrateExcels(START_ROW, END_ROW, BUNDLE_SIZE);
//            excelHelper.createCompareSet(START_ROW, END_ROW);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
