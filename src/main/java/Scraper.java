import com.fasterxml.jackson.databind.ObjectMapper;
import dto.*;
import org.apache.commons.math3.util.Pair;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.chrome.ChromeDriver;
import scraper.CentralLibScraper;
import scraper.CongressScraper;
import scraper.KerisScraper;
import util.Selenium;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Scraper {
    public static final int ID_ROW_INDEX = 1;
    public static final int NAME_ROW_INDEX = 2;
    public static final int COLLEGE_ROW_INDEX = 4;
    public static final int CLAIM_CODE_ROW_INDEX = 15;
    public static final int CONTROL_CODE_ROW_INDEX = 16;

    public static final String RESOURCE_DIRECTORY = "/Users/kdpark/Documents/side_project/seojae/java_ver/src/main/resources/";
    public static final String RESULT_FILE_NAME = "result";
    public static final String ORIGIN_FILE_NAME = "data.xlsx";

    private final ObjectMapper mapper;
    private final ExcelHelper excelHelper;

    public Scraper() {
        this.mapper = new ObjectMapper();
        this.excelHelper = new ExcelHelper();
    }

    public void start(int startRowNum, int endRowNum, int bundleSize) {
        Workbook excel = excelHelper.getExcelData(ORIGIN_FILE_NAME);
        if (excel == null) return;

        Sheet sheet = excel.getSheetAt(0);
        int totalRowCount = sheet.getPhysicalNumberOfRows();
        if (startRowNum > endRowNum || startRowNum > totalRowCount || endRowNum > totalRowCount) return;

        int bundleCount = (endRowNum - startRowNum + 1) / bundleSize;
        if ((endRowNum - startRowNum + 1) % bundleSize != 0)
            bundleCount++;

        List<List<Row>> bundles = new ArrayList<>();

        for (int i = 0; i < bundleCount; i++) {
            bundles.add(new ArrayList<>());
        }

        System.out.printf("rowCount: %d, bundleSize: %d,bundleCount: %d%n", bundleCount, endRowNum - startRowNum + 1, bundleSize);

        int bundleIndex = 0;
        for (int i = startRowNum - 1; i < endRowNum; i++) {
            Row row = sheet.getRow(i);
            bundles.get(bundleIndex).add(row);
            if (bundles.get(bundleIndex).size() == bundleSize)
                bundleIndex++;
        }

        for (int i = 0; i < bundles.size(); i++) {
            int finalI = i;
            new Thread(() -> {
                try {
                    start(bundles.get(finalI), finalI);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private void start(List<Row> rows, int fileIndex) throws IOException {
        List<Paper> list = new ArrayList<>();

        Workbook resultExcel = new XSSFWorkbook();
        Sheet resultSheet = resultExcel.createSheet("시트1");
        excelHelper.createExcelHeaders(resultSheet);

        ChromeDriver driver = Selenium.newInstance();

        for (int i = 0; i < rows.size(); i++) {
            Row row  = rows.get(i);
            if (row == null) break;

            int id = (int) row.getCell(ID_ROW_INDEX).getNumericCellValue();
            String paperName = row.getCell(NAME_ROW_INDEX).getStringCellValue();
            String college = row.getCell(COLLEGE_ROW_INDEX).getStringCellValue();
            String claimCode = row.getCell(CLAIM_CODE_ROW_INDEX).getStringCellValue();
            String controlCode = row.getCell(CONTROL_CODE_ROW_INDEX).getStringCellValue();

            System.out.printf("%d : %s%n", id, paperName);

            Paper paper = new Paper(id, paperName);

            // 국회도서관
            Pair<Congress, CommonInfo> congressPair = CongressScraper.scrap(driver, controlCode, paperName);
            congressPair.getFirst().setClaimCode(claimCode);

            if (congressPair.getSecond().getFullName() != null)
                paperName = congressPair.getSecond().getFullName();

            // Riss
            Pair<Keris, CommonInfo> kerisPair = KerisScraper.scrap(driver, paperName, college);

            // 중앙도서관
            CentralLib centralLib = CentralLibScraper.scrap(driver, paperName);

            paper.setCongress(congressPair.getFirst());
            paper.setKeris(kerisPair.getFirst());
            paper.setCentralLib(centralLib);
            paper.setInfo(congressPair.getSecond() != null ? congressPair.getSecond() : kerisPair.getSecond());

            list.add(paper);
            excelHelper.writeToExcel(resultSheet, i, paper);

            if (i % 10 == 0) {
                mapper.writerWithDefaultPrettyPrinter()
                        .writeValue(new File(RESOURCE_DIRECTORY + RESULT_FILE_NAME + ".json"), list);
                excelHelper.saveExcel(resultExcel, RESULT_FILE_NAME + ".xlsx");
            }
        }

        if (list.size() > 0) {
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(RESOURCE_DIRECTORY + RESULT_FILE_NAME + ".json"), list);
            excelHelper.saveExcel(resultExcel, RESULT_FILE_NAME + ".xlsx");
        }
    }
}
