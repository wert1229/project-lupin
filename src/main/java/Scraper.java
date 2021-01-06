import com.fasterxml.jackson.databind.ObjectMapper;
import dto.*;
import org.apache.commons.math3.util.Pair;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.chrome.ChromeDriver;
import scraper.CentralLibScraper;
import scraper.CongressScraper;
import scraper.KerisScraper;
import util.Selenium;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class Scraper {
    public static final String EXCEL_FILE_PATH = "data.xlsx";

    public static final int ID_ROW_INDEX = 1;
    public static final int NAME_ROW_INDEX = 2;
    public static final int COLLEGE_ROW_INDEX = 4;
    public static final int CLAIM_CODE_ROW_INDEX = 15;
    public static final int CONTROL_CODE_ROW_INDEX = 16;

    public static final String RESULT_FILE_PATH = "/Users/kdpark/Documents/side_project/seojae/java_ver/src/main/resources/";

    private final ObjectMapper mapper;

    public Scraper() {
        this.mapper = new ObjectMapper();
    }

    public void start(int startRowNum, int endRowNum, int bundleSize) {
        Workbook excel = getExcelData();
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

        System.out.println(bundleCount);
        System.out.println(endRowNum - startRowNum + 1);
        System.out.println(bundleSize);
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
                    start(bundles.get(finalI), "result" + finalI);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private void start(List<Row> rows, String fileName) throws IOException {
        List<Paper> list = new ArrayList<>();

        Workbook resultExcel = new XSSFWorkbook();
        Sheet resultSheet = resultExcel.createSheet("시트1");
        createExcelHeaders(resultSheet);

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
            writeToExcel(resultSheet, i, paper);

            if (i % 10 == 0) {
                mapper.writerWithDefaultPrettyPrinter()
                        .writeValue(new File(RESULT_FILE_PATH + fileName + ".json"), list);
                saveExcel(resultExcel, fileName + ".xlsx");
            }
        }

        if (list.size() > 0) {
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(RESULT_FILE_PATH + fileName + ".json"), list);
            saveExcel(resultExcel, fileName + ".xlsx");
        }
    }

    private void saveExcel(Workbook result, String fileName) throws IOException {
        File file = new File(RESULT_FILE_PATH + fileName);
        FileOutputStream fos = new FileOutputStream(file);
        result.write(fos);
    }

    private void createExcelHeaders(Sheet sheet) {
        Row header = sheet.createRow(0);
        header.createCell(2).setCellValue("서지사항");
        header.createCell(7).setCellValue("소장사항1 (국회도서관)");
        header.createCell(14).setCellValue("소장사항2 (국립중앙도서관)");
        header.createCell(21).setCellValue("소장사항3 (KERIS)");

        CellRangeAddress common = new CellRangeAddress(0, 0, 2, 6);
        CellRangeAddress congress = new CellRangeAddress(0, 0, 7, 13);
        CellRangeAddress centralLib = new CellRangeAddress(0, 0, 14, 20);
        CellRangeAddress keris = new CellRangeAddress(0, 0, 21, 25);
        sheet.addMergedRegion(common);
        sheet.addMergedRegion(congress);
        sheet.addMergedRegion(centralLib);
        sheet.addMergedRegion(keris);

        Row subHeader = sheet.createRow(1);
        subHeader.createCell(0).setCellValue("순번");
        subHeader.createCell(1).setCellValue("서명");

        subHeader.createCell(2).setCellValue("크기");
        subHeader.createCell(3).setCellValue("페이지");
        subHeader.createCell(4).setCellValue("학위유형");
        subHeader.createCell(5).setCellValue("전공");
        subHeader.createCell(6).setCellValue("지도교수");

        subHeader.createCell(7).setCellValue("소장처");
        subHeader.createCell(8).setCellValue("원문소장");
        subHeader.createCell(9).setCellValue("실물소장");
        subHeader.createCell(10).setCellValue("서비스형태");
        subHeader.createCell(11).setCellValue("청구기호");
        subHeader.createCell(12).setCellValue("제어번호");
        subHeader.createCell(13).setCellValue("원문URL");

        subHeader.createCell(14).setCellValue("소장처");
        subHeader.createCell(15).setCellValue("원문소장");
        subHeader.createCell(16).setCellValue("실물소장");
        subHeader.createCell(17).setCellValue("서비스형태");
        subHeader.createCell(18).setCellValue("청구기호");
        subHeader.createCell(19).setCellValue("원문URL");
        subHeader.createCell(20).setCellValue("유사도");

        subHeader.createCell(21).setCellValue("소장처");
        subHeader.createCell(22).setCellValue("원문소장");
        subHeader.createCell(23).setCellValue("서비스형태");
        subHeader.createCell(24).setCellValue("원문URL");
        subHeader.createCell(25).setCellValue("유사도");
    }

    private void writeToExcel(Sheet sheet, int index, Paper paper) {
        int headerOffSet = 2;
        Row row = sheet.createRow(headerOffSet + index);

        row.createCell(0).setCellValue(paper.getId());
        row.createCell(1).setCellValue(paper.getPaperName());

        row.createCell(2).setCellValue(paper.getInfo().getSize());
        row.createCell(3).setCellValue(paper.getInfo().getPage());
        row.createCell(4).setCellValue(paper.getInfo().getDegree());
        row.createCell(5).setCellValue(paper.getInfo().getMajor());
        row.createCell(6).setCellValue(paper.getInfo().getProfessor());

        row.createCell(7).setCellValue(paper.getCongress().getOrganName());
        row.createCell(8).setCellValue(paper.getCongress().isDigital() ? "O" : "X");
        row.createCell(9).setCellValue(paper.getCongress().isOriginal() ? "O" : "X");
        row.createCell(10).setCellValue(paper.getCongress().getServiceMethod());
        row.createCell(11).setCellValue(paper.getCongress().getClaimCode());
        row.createCell(12).setCellValue(paper.getCongress().getControlCode());
        row.createCell(13).setCellValue(paper.getCongress().getDigitalUrl());

        row.createCell(14).setCellValue(paper.getCentralLib().getOrganName());
        row.createCell(15).setCellValue(paper.getCentralLib().isDigital() ? "O" : "X");
        row.createCell(16).setCellValue(paper.getCentralLib().isOriginal() ? "O" : "X");
        row.createCell(17).setCellValue(paper.getCentralLib().getServiceMethod());
        row.createCell(18).setCellValue(paper.getCentralLib().getClaimCode());
        row.createCell(19).setCellValue(paper.getCentralLib().getDigitalUrl());
        row.createCell(20).setCellValue(paper.getCentralLib().getJaccard());

        row.createCell(21).setCellValue(paper.getKeris().getOrganName());
        row.createCell(22).setCellValue(paper.getKeris().isDigital() ? "O" : "X");
        row.createCell(23).setCellValue(paper.getKeris().getServiceMethod());
        row.createCell(24).setCellValue(paper.getKeris().getDigitalUrl());
        row.createCell(25).setCellValue(paper.getKeris().getJaccard());
    }

    private Workbook getExcelData() {
        InputStream is = getClass().getResourceAsStream(EXCEL_FILE_PATH);
        try {
            return new XSSFWorkbook(is);
        } catch (IOException e) {
            return null;
        }
    }
}
