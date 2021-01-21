import util.ScrapUtil;
import util.Selenium;

public class Main {

    public static final int START_ROW = 267;
    public static final int END_ROW = 781;
    public static final int BUNDLE_SIZE = 90;

    public static void main(String[] args) {
        Scraper scraper = new Scraper();
        ExcelHelper excelHelper = new ExcelHelper();
        PdfHelper pdfHelper = new PdfHelper();

        try {
            scraper.start(START_ROW, END_ROW, BUNDLE_SIZE);
//            pdfHelper.renamePdfFile(START_ROW, END_ROW, BUNDLE_SIZE);
//            excelHelper.integrateExcels(START_ROW, END_ROW, BUNDLE_SIZE);
//            excelHelper.createCompareSet(START_ROW, END_ROW);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Selenium.quit();
    }
}
