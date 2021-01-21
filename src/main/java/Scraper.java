import com.fasterxml.jackson.databind.ObjectMapper;
import dto.*;
import org.apache.commons.math3.util.Pair;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.FluentWait;
import scraper.CentralLibScraper;
import scraper.CongressScraper;
import scraper.KerisScraper;
import util.Selenium;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Scraper {
    public static final int ID_ROW_INDEX = 1;
    public static final int NAME_ROW_INDEX = 2;
    public static final int AUTHOR_ROW_INDEX = 3;
    public static final int CLAIM_CODE_ROW_INDEX = 15;
    public static final int CONTROL_CODE_ROW_INDEX = 16;
    public static final int REGISTER_CODE_ROW_INDEX = 17;

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
        int lastRowNum = sheet.getLastRowNum();
        if (startRowNum > endRowNum || startRowNum - 1 > lastRowNum || endRowNum - 1 > lastRowNum) return;

        int bundleCount = (endRowNum - startRowNum + 1) / bundleSize;
        if ((endRowNum - startRowNum + 1) % bundleSize != 0)
            bundleCount++;

        List<List<Row>> bundles = new ArrayList<>();

        for (int i = 0; i < bundleCount; i++) {
            bundles.add(new ArrayList<>());
        }

        System.out.printf("rowCount: %d, bundleSize: %d,bundleCount: %d%n", endRowNum - startRowNum + 1, bundleSize, bundleCount);

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
            if (row == null || row.getCell(NAME_ROW_INDEX) == null) continue;

            int id = (int) row.getCell(ID_ROW_INDEX).getNumericCellValue();
            String paperName = row.getCell(NAME_ROW_INDEX).getStringCellValue();
            String author = row.getCell(AUTHOR_ROW_INDEX).getStringCellValue();
            String claimCode = row.getCell(CLAIM_CODE_ROW_INDEX).getStringCellValue();
            String controlCode = row.getCell(CONTROL_CODE_ROW_INDEX).getStringCellValue();

            Cell registerCodeCell = row.getCell(REGISTER_CODE_ROW_INDEX);
            String registerCode = "";
            if (registerCodeCell.getCellType() == CellType.STRING) {
                registerCode = registerCodeCell.getStringCellValue();
            } else {
                registerCode = new DecimalFormat("0.#").format(registerCodeCell.getNumericCellValue()).trim();
                registerCode = registerCode.equals("0") ? "" : registerCode;
            }

            if (paperName == null || paperName.equals("")) continue;

            System.out.printf("%d : %d of %d : %s%n", id, i + 1, rows.size(), paperName);

            Paper paper = new Paper(id, paperName, author);

            // 국회도서관
            Pair<Congress, CommonInfo> congressPair = null;
            int tried = 1;
            while (tried != 0) {
                try {
                    congressPair = CongressScraper.scrap(driver, controlCode, paperName);
                    tried = 0;
                } catch (NoSuchElementException e) {
                    System.out.println("congress NoSuchElement retried "+ tried +" times : " + paperName);
                    System.out.println(e.getMessage());
                    tried++;
                }
            }
            congressPair.getFirst().setClaimCode(claimCode);
            congressPair.getFirst().setRegisterCode(registerCode);

            String congressPaperName = null;
            if (congressPair.getSecond().getFullName() != null) {
                congressPaperName = congressPair.getSecond().getFullName();
            }

            // Riss
            Pair<Keris, CommonInfo> kerisPair = null;
            tried = 1;
            while (tried != 0) {
                try {
                    kerisPair = KerisScraper.scrap(driver, paperName, congressPaperName, author);
                    tried = 0;
                } catch (Exception e) {
                    System.out.println("keris Exception retried "+ tried +" times : " + paperName);
                    System.out.println(e.getMessage());
                    tried++;
                }
            }

            // 중앙도서관
            CentralLib centralLib = null;
            tried = 1;
            while (tried != 0) {
                try {
                    centralLib = CentralLibScraper.scrap(driver, paperName, congressPaperName, author);
                    tried = 0;
                } catch (Exception e) {
                    System.out.println("centralLib Exception retried "+ tried +" times : " + paperName);
                    System.out.println(e.getMessage());
                    tried++;
                }
            }

            paper.setCongress(congressPair.getFirst());
            paper.setKeris(kerisPair.getFirst());
            paper.setCentralLib(centralLib);
            paper.setInfo(createInfo(congressPair.getSecond(), kerisPair.getSecond()));

            list.add(paper);
            excelHelper.writeToExcel(resultSheet, i, paper);

            if (i % 10 == 0) {
                mapper.writerWithDefaultPrettyPrinter()
                        .writeValue(new File(RESOURCE_DIRECTORY + RESULT_FILE_NAME + fileIndex + ".json"), list);
                excelHelper.saveExcel(resultExcel, RESULT_FILE_NAME + fileIndex + ".xlsx");
            }
        }

        waitForDownload(driver);

        if (list.size() > 0) {
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(RESOURCE_DIRECTORY + RESULT_FILE_NAME + fileIndex + ".json"), list);
            excelHelper.saveExcel(resultExcel, RESULT_FILE_NAME + fileIndex + ".xlsx");
        }

        driver.quit();
    }

    private void waitForDownload(ChromeDriver driver) {
        driver.get("chrome://downloads/");
        List<String> list = new FluentWait<JavascriptExecutor>(driver)
                .withTimeout(Duration.ofSeconds(120))
                .until(javascriptExecutor -> (List<String>) javascriptExecutor.executeScript("var items = document.querySelector('downloads-manager')" +
                        ".shadowRoot.getElementById('downloadsList').items;" +
                        "if (items.every(e => e.state === 'COMPLETE'))" +
                        "return items.map(e => e.fileUrl || e.file_url);"));
    }

    private void changeFiles(List<Paper> list) {
        for (Paper paper : list) {
            String paperName = paper.getPaperName();
            String author = paper.getAuthor();
            Keris keris = paper.getKeris();
            String newFileName = getNewFileName(paperName, author);
            changeFileName(keris.getFileName(), newFileName);
        }
    }

    private String getNewFileName(String paperName, String author) {
        int spaceCount = 0;
        for (int i = 0; i < paperName.length(); i++) {
            if (paperName.charAt(i) == ' ') {
                spaceCount++;
            }

            if (spaceCount == 3) {
                return paperName.substring(0, i).trim() + "-" + author + ".pdf";
            }
        }
        return paperName + "-" + author + ".pdf";
    }

    private void changeFileName(String fileName, String newFileName) {
        if (fileName == null || fileName.equals("")) return;
        File originFile = new File(Selenium.DOWNLOAD_PATH + fileName);
        boolean isSuccess = originFile.renameTo(new File(Selenium.DOWNLOAD_PATH + "/renamed/" + newFileName));
    }

    private CommonInfo createInfo(CommonInfo congress, CommonInfo keris) {
        CommonInfo info = new CommonInfo();
        info.setFullName(congress.getFullName() != null && !congress.getFullName().equals("") ? congress.getFullName() : keris.getFullName());
        info.setMajor(congress.getMajor() != null && !congress.getMajor().equals("") ? congress.getMajor() : keris.getMajor());
        info.setProfessor(congress.getProfessor() != null && !congress.getProfessor().equals("") ? congress.getProfessor() : keris.getProfessor());
        info.setSize(congress.getSize() != null && !congress.getSize().equals("") ? congress.getSize() : keris.getSize());
        info.setPage(congress.getPage() != null && !congress.getPage().equals("") ? congress.getPage() : keris.getPage());
        info.setDegree(congress.getDegree() != null && !congress.getDegree().equals("") ? congress.getDegree() : keris.getDegree());
        return info;
    }
}
