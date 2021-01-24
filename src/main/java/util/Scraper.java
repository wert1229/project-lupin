package util;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import scraper.ScrapJob;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Scraper {
    public static final String ORIGIN_FILE_NAME = "data.xlsx";

    private final ExcelHelper excelHelper;

    public Scraper() {
        this.excelHelper = new ExcelHelper();
    }

    public void start(int startRowNum, int endRowNum, int bundleSize) {
        Workbook excel = excelHelper.getExcelData(ORIGIN_FILE_NAME);
        if (excel == null) return;

        Sheet sheet = excel.getSheetAt(0);
        int lastRowNum = sheet.getLastRowNum();
        if (startRowNum > endRowNum || startRowNum - 1 > lastRowNum || endRowNum - 1 > lastRowNum)
            return;

        List<List<Row>> bundles = getBundleList(sheet, startRowNum, endRowNum, bundleSize);

        System.out.printf("rowCount: %d, bundleSize: %d,bundleCount: %d%n", endRowNum - startRowNum + 1, bundleSize, bundles.size());

        createAndRunThread(bundles);
    }

    private List<List<Row>> getBundleList(Sheet sheet, int startRowNum, int endRowNum, int bundleSize) {
        int bundleCount = getBundleCount(startRowNum, endRowNum, bundleSize);
        List<List<Row>> bundles = emptyBundleList(bundleCount);

        int bundleIndex = 0;
        for (int i = startRowNum - 1; i < endRowNum; i++) {
            Row row = sheet.getRow(i);
            bundles.get(bundleIndex).add(row);
            if (bundles.get(bundleIndex).size() == bundleSize)
                bundleIndex++;
        }
        return bundles;
    }

    private List<List<Row>> emptyBundleList(int bundleCount) {
        List<List<Row>> bundles = new ArrayList<>();
        for (int i = 0; i < bundleCount; i++) {
            bundles.add(new ArrayList<>());
        }
        return bundles;
    }

    private int getBundleCount(int startRowNum, int endRowNum, int bundleSize) {
        int bundleCount = (endRowNum - startRowNum + 1) / bundleSize;
        if ((endRowNum - startRowNum + 1) % bundleSize != 0) {
            bundleCount++;
        }
        return bundleCount;
    }

    private void createAndRunThread(List<List<Row>> bundles) {
        for (int i = 0; i < bundles.size(); i++) {
            int finalI = i;
            new Thread(() -> {
                try {
                    new ScrapJob(finalI, bundles.get(finalI)).scrap();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
