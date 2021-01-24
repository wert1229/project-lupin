import dto.Keris;
import dto.Paper;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import util.ScrapUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ExcelHelper {
    public static final int EXCEL_HEADER_OFFSET = 2;
    public static final String INTEGRATION_SHEET_NAME = "시트1";
    public static final String INTEGRATION_FILE_NAME = "integration.xlsx";
    public static final String COMPARE_FILE_NAME = "compare.xlsx";

    private CellStyle emptyHighlight;

    public Workbook getExcelData(String fileName) {
        InputStream is = getClass().getResourceAsStream(fileName);
        try {
            return new XSSFWorkbook(is);
        } catch (IOException e) {
            return null;
        }
    }

    public void integrateExcels(int startRowNum, int endRowNum, int bundleSize) throws IOException {
        Workbook integration = new XSSFWorkbook();
        Sheet sheet = integration.createSheet(INTEGRATION_SHEET_NAME);
        createExcelHeaders(sheet);
        createEmptyStyle(integration);

        int bundleCount = (endRowNum - startRowNum + 1) / bundleSize;
        if ((endRowNum - startRowNum + 1) % bundleSize != 0) {
            bundleCount++;
        }
        System.out.println(bundleCount);

        for (int i = 0; i < bundleCount; i++) {
            Workbook excel = getExcelData(Scraper.RESULT_FILE_NAME + i + ".xlsx");
            appendExcelData(sheet, excel);
        }

        saveExcel(integration, INTEGRATION_FILE_NAME);
    }

    private void createEmptyStyle(Workbook excel) {
        CellStyle emptyHighlight = excel.createCellStyle();
        emptyHighlight.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        emptyHighlight.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        this.emptyHighlight = emptyHighlight;
    }

    private void appendExcelData(Sheet baseSheet, Workbook excel) {
        Sheet addSheet = excel.getSheetAt(0);

        int addCount = addSheet.getPhysicalNumberOfRows();
        int integrationCount = baseSheet.getPhysicalNumberOfRows();

        for (int i = 0; i < addCount - EXCEL_HEADER_OFFSET; i++) {
            Row row = addSheet.getRow(i + EXCEL_HEADER_OFFSET);
            appendRow(baseSheet, integrationCount + i, row);
        }
    }

    private void appendRow(Sheet baseSheet, int index, Row addRow) {
        if (addRow == null) {
            return;
        }

        Row row = baseSheet.createRow(index);

        for (Cell addRowCell : addRow) {
            if (addRowCell.getCellType() == CellType.NUMERIC) {
                row.createCell(row.getPhysicalNumberOfCells()).setCellValue(addRowCell.getNumericCellValue());
            } else {
                Cell newCell = row.createCell(row.getPhysicalNumberOfCells());
                newCell.setCellValue(addRowCell.getStringCellValue());
                if (newCell.getColumnIndex() <= 6 && StringUtils.isBlank(addRowCell.getStringCellValue())) {
                    newCell.setCellStyle(this.emptyHighlight);
                }
                if (newCell.getColumnIndex() >= 27 && newCell.getColumnIndex() <= 28 && !StringUtils.isBlank(addRowCell.getStringCellValue())) {
                    newCell.setCellStyle(this.emptyHighlight);
                }
            }
        }
    }

    public void createExcelHeaders(Sheet sheet) {
        Row header = sheet.createRow(0);
        header.createCell(2).setCellValue("서지사항");
        header.createCell(7).setCellValue("소장사항1 (국회도서관)");
        header.createCell(15).setCellValue("소장사항2 (국립중앙도서관)");
        header.createCell(21).setCellValue("소장사항3 (KERIS)");

        CellRangeAddress common = new CellRangeAddress(0, 0, 2, 6);
        CellRangeAddress congress = new CellRangeAddress(0, 0, 7, 14);
        CellRangeAddress centralLib = new CellRangeAddress(0, 0, 15, 20);
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
        subHeader.createCell(13).setCellValue("등록번호");
        subHeader.createCell(14).setCellValue("원문URL");

        subHeader.createCell(15).setCellValue("소장처");
        subHeader.createCell(16).setCellValue("원문소장");
        subHeader.createCell(17).setCellValue("실물소장");
        subHeader.createCell(18).setCellValue("서비스형태");
        subHeader.createCell(19).setCellValue("청구기호");
        subHeader.createCell(20).setCellValue("원문URL");

        subHeader.createCell(21).setCellValue("소장처");
        subHeader.createCell(22).setCellValue("원문소장");
        subHeader.createCell(23).setCellValue("서비스형태");
        subHeader.createCell(24).setCellValue("원문URL");

        subHeader.createCell(25).setCellValue("중도 유사도");
        subHeader.createCell(26).setCellValue("RISS 유사도");
        subHeader.createCell(27).setCellValue("중도 저자비교");
        subHeader.createCell(28).setCellValue("RISS 저자비교");
    }

    public void saveExcel(Workbook result, String fileName) throws IOException {
        File file = new File(Scraper.RESOURCE_DIRECTORY + fileName);
        FileOutputStream fos = new FileOutputStream(file);
        result.write(fos);
    }

    public void writeToExcel(Sheet sheet, int index, Paper paper) {
        Row row = sheet.createRow(index + EXCEL_HEADER_OFFSET);

        row.createCell(0).setCellValue(paper.getId());
        row.createCell(1).setCellValue(paper.getPaperName());

        row.createCell(2).setCellValue(paper.getInfo().getSize());
        row.createCell(3).setCellValue(paper.getInfo().getPage());
        row.createCell(4).setCellValue(paper.getInfo().getDegree());
        row.createCell(5).setCellValue(paper.getInfo().getMajor());
        row.createCell(6).setCellValue(paper.getInfo().getProfessor());

        row.createCell(7).setCellValue(paper.getCongress().getOrganName());
        row.createCell(8).setCellValue(ScrapUtil.getExcelValue(paper.getCongress().getDigital()));
        row.createCell(9).setCellValue(ScrapUtil.getExcelValue(paper.getCongress().getOriginal()));
        row.createCell(10).setCellValue(paper.getCongress().getServiceMethod());
        row.createCell(11).setCellValue(paper.getCongress().getClaimCode());
        row.createCell(12).setCellValue(paper.getCongress().getControlCode());
        row.createCell(13).setCellValue(paper.getCongress().getRegisterCode());
        row.createCell(14).setCellValue(paper.getCongress().getDigitalUrl());

        row.createCell(15).setCellValue(paper.getCentralLib().getOrganName());
        row.createCell(16).setCellValue(ScrapUtil.getExcelValue(paper.getCentralLib().getDigital()));
        row.createCell(17).setCellValue(ScrapUtil.getExcelValue(paper.getCentralLib().getOriginal()));
        row.createCell(18).setCellValue(paper.getCentralLib().getServiceMethod());
        row.createCell(19).setCellValue(paper.getCentralLib().getClaimCode());
        row.createCell(20).setCellValue(paper.getCentralLib().getDigitalUrl());

        row.createCell(21).setCellValue(paper.getKeris().getOrganName());
        row.createCell(22).setCellValue(ScrapUtil.getExcelValue((paper.getKeris().getDigital())));
        row.createCell(23).setCellValue(paper.getKeris().getServiceMethod());
        row.createCell(24).setCellValue(paper.getKeris().getDigitalUrl());

        row.createCell(25).setCellValue(paper.getCentralLib().getSimilarity());
        row.createCell(26).setCellValue(paper.getKeris().getSimilarity());
        row.createCell(27).setCellValue(paper.getCentralLib().getAuthorDiff());
        row.createCell(28).setCellValue(paper.getKeris().getAuthorDiff());
    }

    public void createCompareSet(int startRowNum, int endRowNum) throws IOException {
        Workbook compareSet = new XSSFWorkbook();
        Sheet sheet = compareSet.createSheet("시트1");
        createExcelCompareHeaders(sheet);

        Workbook origin = getExcelData(Scraper.ORIGIN_FILE_NAME);
        Workbook comp = getExcelData(INTEGRATION_FILE_NAME);

        Sheet originSheet = origin.getSheetAt(0);
        Sheet compSheet = comp.getSheetAt(0);

        for (int i = 0; i < endRowNum - startRowNum + 1; i++) {
            Row originRow = originSheet.getRow(startRowNum + i - 1);
            Row compRow = compSheet.getRow(EXCEL_HEADER_OFFSET + i);
            addCompareRow(sheet, i, originRow, compRow);
        }

        saveExcel(compareSet, COMPARE_FILE_NAME);
    }

    private void addCompareRow(Sheet sheet, int index, Row originRow, Row compRow) {
        Row row = sheet.createRow(EXCEL_HEADER_OFFSET + index);

        row.createCell(0).setCellValue(getCellValue(compRow.getCell(0)));
        row.createCell(1).setCellValue(getCellValue(compRow.getCell(1)));

        row.createCell(2).setCellValue(getCellValue(originRow.getCell(6)));
        row.createCell(3).setCellValue(getCellValue(compRow.getCell(2)));
        row.createCell(4).setCellValue(getCellValue(originRow.getCell(7)));
        row.createCell(5).setCellValue(getCellValue(compRow.getCell(3)));
        row.createCell(6).setCellValue(getCellValue(originRow.getCell(8)));
        row.createCell(7).setCellValue(getCellValue(compRow.getCell(4)));
        row.createCell(8).setCellValue(getCellValue(originRow.getCell(9)));
        row.createCell(9).setCellValue(getCellValue(compRow.getCell(5)));
        row.createCell(10).setCellValue(getCellValue(originRow.getCell(10)));
        row.createCell(11).setCellValue(getCellValue(compRow.getCell(6)));

        row.createCell(12).setCellValue(getCellValue(originRow.getCell(11)));
        row.createCell(13).setCellValue(getCellValue(originRow.getCell(12)));
        row.createCell(14).setCellValue(getCellValue(compRow.getCell(8)));
        row.createCell(15).setCellValue(getCellValue(originRow.getCell(13)));
        row.createCell(16).setCellValue(getCellValue(compRow.getCell(9)));
        row.createCell(17).setCellValue(getCellValue(originRow.getCell(14)));
        row.createCell(18).setCellValue(getCellValue(compRow.getCell(10)));
        row.createCell(19).setCellValue(getCellValue(originRow.getCell(18)));
        row.createCell(20).setCellValue(getCellValue(compRow.getCell(14)));

        row.createCell(21).setCellValue(getCellValue(originRow.getCell(19)));
        row.createCell(22).setCellValue(getCellValue(originRow.getCell(20)));
        row.createCell(23).setCellValue(getCellValue(compRow.getCell(16)));
        row.createCell(24).setCellValue(getCellValue(originRow.getCell(21)));
        row.createCell(25).setCellValue(getCellValue(compRow.getCell(17)));
        row.createCell(26).setCellValue(getCellValue(originRow.getCell(22)));
        row.createCell(27).setCellValue(getCellValue(compRow.getCell(18)));
        row.createCell(28).setCellValue(getCellValue(originRow.getCell(23)));
        row.createCell(29).setCellValue(getCellValue(compRow.getCell(19)));
        row.createCell(30).setCellValue(getCellValue(originRow.getCell(24)));
        row.createCell(31).setCellValue(getCellValue(compRow.getCell(20)));
        row.createCell(32).setCellValue(getCellValue(compRow.getCell(25)));

        row.createCell(33).setCellValue(getCellValue(originRow.getCell(25)));
        row.createCell(34).setCellValue(getCellValue(originRow.getCell(26)));
        row.createCell(35).setCellValue(getCellValue(compRow.getCell(22)));
        row.createCell(36).setCellValue(getCellValue(originRow.getCell(27)));
        row.createCell(37).setCellValue(getCellValue(compRow.getCell(23)));
        row.createCell(38).setCellValue(getCellValue(originRow.getCell(28)));
        row.createCell(39).setCellValue(getCellValue(compRow.getCell(24)));
        row.createCell(40).setCellValue(getCellValue(compRow.getCell(26)));
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.NUMERIC)
            return String.valueOf(cell.getNumericCellValue());
        else
            return cell.getStringCellValue();
    }

    public void createExcelCompareHeaders(Sheet sheet) {
        Row header = sheet.createRow(0);
        header.createCell(2).setCellValue("서지사항");
        header.createCell(12).setCellValue("소장사항1 (국회도서관)");
        header.createCell(21).setCellValue("소장사항2 (국립중앙도서관)");
        header.createCell(33).setCellValue("소장사항3 (KERIS)");

        CellRangeAddress common = new CellRangeAddress(0, 0, 2, 11);
        CellRangeAddress congress = new CellRangeAddress(0, 0, 12, 20);
        CellRangeAddress centralLib = new CellRangeAddress(0, 0, 21, 32);
        CellRangeAddress keris = new CellRangeAddress(0, 0, 33, 40);
        sheet.addMergedRegion(common);
        sheet.addMergedRegion(congress);
        sheet.addMergedRegion(centralLib);
        sheet.addMergedRegion(keris);

        Row subHeader = sheet.createRow(1);

        final String originPostfix = "(원)";
        final String compPostfix = "(비)";

        subHeader.createCell(0).setCellValue("순번");
        subHeader.createCell(1).setCellValue("서명");

        subHeader.createCell(2).setCellValue("크기" + originPostfix);
        subHeader.createCell(3).setCellValue("크기" + compPostfix);
        subHeader.createCell(4).setCellValue("페이지" + originPostfix);
        subHeader.createCell(5).setCellValue("페이지" + compPostfix);
        subHeader.createCell(6).setCellValue("학위유형" + originPostfix);
        subHeader.createCell(7).setCellValue("학위유형" + compPostfix);
        subHeader.createCell(8).setCellValue("전공" + originPostfix);
        subHeader.createCell(9).setCellValue("전공" + compPostfix);
        subHeader.createCell(10).setCellValue("지도교수" + originPostfix);
        subHeader.createCell(11).setCellValue("지도교수" + compPostfix);

        subHeader.createCell(12).setCellValue("소장처");
        subHeader.createCell(13).setCellValue("원문소장" + originPostfix);
        subHeader.createCell(14).setCellValue("원문소장" + compPostfix);
        subHeader.createCell(15).setCellValue("실물소장" + originPostfix);
        subHeader.createCell(16).setCellValue("실물소장" + compPostfix);
        subHeader.createCell(17).setCellValue("서비스형태" + originPostfix);
        subHeader.createCell(18).setCellValue("서비스형태" + compPostfix);
        subHeader.createCell(19).setCellValue("원문URL" + originPostfix);
        subHeader.createCell(20).setCellValue("원문URL" + compPostfix);

        subHeader.createCell(21).setCellValue("소장처");
        subHeader.createCell(22).setCellValue("원문소장" + originPostfix);
        subHeader.createCell(23).setCellValue("원문소장" + compPostfix);
        subHeader.createCell(24).setCellValue("실물소장" + originPostfix);
        subHeader.createCell(25).setCellValue("실물소장" + compPostfix);
        subHeader.createCell(26).setCellValue("서비스형태" + originPostfix);
        subHeader.createCell(27).setCellValue("서비스형태" + compPostfix);
        subHeader.createCell(28).setCellValue("청구기호" + originPostfix);
        subHeader.createCell(29).setCellValue("청구기호" + compPostfix);
        subHeader.createCell(30).setCellValue("원문URL" + originPostfix);
        subHeader.createCell(31).setCellValue("원문URL" + compPostfix);
        subHeader.createCell(32).setCellValue("유사도");

        subHeader.createCell(33).setCellValue("소장처");
        subHeader.createCell(34).setCellValue("원문소장" + originPostfix);
        subHeader.createCell(35).setCellValue("원문소장" + compPostfix);
        subHeader.createCell(36).setCellValue("서비스형태" + originPostfix);
        subHeader.createCell(37).setCellValue("서비스형태" + compPostfix);
        subHeader.createCell(38).setCellValue("원문URL" + originPostfix);
        subHeader.createCell(39).setCellValue("원문URL" + compPostfix);
        subHeader.createCell(40).setCellValue("유사도");
    }
}
