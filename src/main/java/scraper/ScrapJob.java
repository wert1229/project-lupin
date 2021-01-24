package scraper;

import com.fasterxml.jackson.databind.ObjectMapper;
import dto.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Pair;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.chrome.ChromeDriver;
import util.ExcelHelper;
import util.Selenium;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class ScrapJob {
    public static final int ID_ROW_INDEX = 1;
    public static final int NAME_ROW_INDEX = 2;
    public static final int AUTHOR_ROW_INDEX = 3;
    public static final int CLAIM_CODE_ROW_INDEX = 15;
    public static final int CONTROL_CODE_ROW_INDEX = 16;
    public static final int REGISTER_CODE_ROW_INDEX = 17;

    public static final String RESOURCE_DIRECTORY = "/Users/kdpark/Documents/side_project/seojae/java_ver/src/main/resources/";
    public static final String RESULT_FILE_NAME = "result";

    private final ObjectMapper mapper;
    private final ExcelHelper excelHelper;
    private final Workbook resultExcel;
    private final Sheet resultSheet;

    private final ChromeDriver driver;
    private final int index;
    private final List<Row> rows;

    public ScrapJob(int index, List<Row> rows) {
        this.driver = Selenium.newInstance();
        this.index = index;
        this.rows = rows;

        this.mapper = new ObjectMapper();
        this.excelHelper = new ExcelHelper();
        this.resultExcel = new XSSFWorkbook();
        this.resultSheet = resultExcel.createSheet("시트1");
        excelHelper.createExcelHeaders(resultSheet);
    }

    public void scrap() throws IOException {
        List<Paper> list = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            Row row  = rows.get(i);
            if (row == null || row.getCell(NAME_ROW_INDEX) == null) {
                continue;
            }

            int id = (int) row.getCell(ID_ROW_INDEX).getNumericCellValue();
            String paperName = row.getCell(NAME_ROW_INDEX).getStringCellValue();
            String author = row.getCell(AUTHOR_ROW_INDEX).getStringCellValue();
            String claimCode = row.getCell(CLAIM_CODE_ROW_INDEX).getStringCellValue();
            String controlCode = row.getCell(CONTROL_CODE_ROW_INDEX).getStringCellValue();
            String registerCode = getRegisterCode(row);

            if (StringUtils.isBlank(paperName)) {
                continue;
            }

            System.out.printf("%d : %d of %d : %s%n", id, i + 1, rows.size(), paperName);

            Paper paper = new Paper(id, paperName, author);

            // 국회도서관
            Pair<Congress, CommonInfo> congressPair = null;
            int tried = 1;
            while (tried != 0) {
                try {
                    congressPair = CongressScraper.scrap(driver, controlCode, paperName);
                    tried = 0;
                } catch (NoSuchElementException | TimeoutException e) {
                    System.out.println("congress NoSuchElement retried "+ tried +" times : " + paperName);
                    System.out.println(e.getMessage());
                    tried++;
                }
            }
            congressPair.getFirst().setClaimCode(claimCode);
            congressPair.getFirst().setRegisterCode(registerCode);

            String congressPaperName = congressPair.getSecond().getFullName() != null ? congressPair.getSecond().getFullName(): null;

            // Riss
            Pair<Keris, CommonInfo> kerisPair = null;
            tried = 1;
            while (tried != 0) {
                try {
                    kerisPair = KerisScraper.scrap(driver, paperName, congressPaperName, author);
                    tried = 0;
                } catch (NoSuchElementException | TimeoutException | ElementNotInteractableException e) {
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
                } catch (NoSuchElementException | TimeoutException | ElementNotInteractableException e) {
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
                saveExcelAndJson(list);
            }
        }

        if (list.size() > 0) {
            saveExcelAndJson(list);
        }

        driver.quit();
    }

    private void saveExcelAndJson(List<Paper> list) throws IOException {
        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(new File(RESOURCE_DIRECTORY + RESULT_FILE_NAME + this.index + ".json"), list);
        excelHelper.saveExcel(resultExcel, RESULT_FILE_NAME + this.index + ".xlsx");
    }

    private String getRegisterCode(Row row) {
        Cell registerCodeCell = row.getCell(REGISTER_CODE_ROW_INDEX);
        String registerCode = "";
        if (registerCodeCell == null) {
            return registerCode;
        }
        if (registerCodeCell.getCellType() == CellType.STRING) {
            registerCode = registerCodeCell.getStringCellValue();
        } else {
            registerCode = new DecimalFormat("0.#").format(registerCodeCell.getNumericCellValue()).trim();
            registerCode = registerCode.equals("0") ? "" : registerCode;
        }
        return registerCode;
    }

    private CommonInfo createInfo(CommonInfo congress, CommonInfo keris) {
        CommonInfo info = new CommonInfo();
        info.setFullName(!StringUtils.isBlank(congress.getFullName()) ? congress.getFullName() : keris.getFullName());
        info.setMajor(!StringUtils.isBlank(congress.getMajor()) ? congress.getMajor() : keris.getMajor());
        info.setProfessor(!StringUtils.isBlank(congress.getProfessor()) ? congress.getProfessor() : keris.getProfessor());
        info.setSize(!StringUtils.isBlank(congress.getSize()) ? congress.getSize() : keris.getSize());
        info.setPage(!StringUtils.isBlank(congress.getPage()) ? congress.getPage() : keris.getPage());
        info.setDegree(!StringUtils.isBlank(congress.getDegree()) ? congress.getDegree() : keris.getDegree());
        return info;
    }
}
