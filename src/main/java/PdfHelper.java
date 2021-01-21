import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dto.Keris;
import dto.Paper;
import util.Selenium;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PdfHelper {

    private ObjectMapper mapper;

    public PdfHelper() {
        this.mapper = new ObjectMapper();
    }

    public void renamePdfFile(int startRowNum, int endRowNum, int bundleSize) throws IOException {
        int bundleCount = (endRowNum - startRowNum + 1) / bundleSize;

        if ((endRowNum - startRowNum + 1) % bundleSize != 0)
            bundleCount++;

        List<Paper> allPapers = new ArrayList<>();
        for (int i = 0; i < bundleCount; i++) {
            List<Paper> papers = getPaper(i);
            allPapers.addAll(papers);
        }

        changeFiles(allPapers);
    }

    private void changeFiles(List<Paper> list) {
        for (int i = 0; i < list.size(); i++) {
            Paper paper = list.get(i);
            String paperName = paper.getPaperName();
            String author = paper.getAuthor();
            Keris keris = paper.getKeris();
            String newFileFolder = "" + (i / 180) + "/";
            String newFileName = getNewFileName(paperName, author);
            changeFileName(keris.getFileName(), newFileFolder + newFileName);
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

    private List<Paper> getPaper(int index) throws IOException {
        String filePath = Scraper.RESOURCE_DIRECTORY + Scraper.RESULT_FILE_NAME + index + ".json";
        return mapper.readValue(new File(filePath), new TypeReference<List<Paper>>(){});
    }
}
