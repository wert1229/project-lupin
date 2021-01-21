package scraper;

import dto.CentralLib;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import util.ScrapUtil;

import java.util.ArrayList;
import java.util.List;

import static util.ScrapUtil.*;

public class CentralLibScraper {

    public static final String CENTRAL_LIB_URL = "https://www.nl.go.kr/NL/contents/search.do?pageNum=1&pageSize=4&srchTarget=total&kwd=";

    public static final String ORGAN_NAME = "국립중앙도서관";
    public static final double JACCARD_STANDARD = 0.65;

    public static String queryPretreatment(String paperName) {
        String query = rearrangeAbnormalSubtitle(paperName);
        query = removeBrackets(query);
        query = changeQuote(query);
        query = query.replaceAll("[≪≫]", " ");
        query = query.replaceAll("[?]", "");
        return query;
    }

    public static String changeQuote(String query) {
        StringBuilder builder = new StringBuilder(query);
        while (builder.indexOf("'") != -1) {
            int index = builder.indexOf("'");
            if (index == 0 || builder.charAt(index - 1) == ' ') {
                builder.replace(index, index + 1, "");
            } else {
                builder.setCharAt(index, '’');
            }
        }
        return builder.toString();
    }

    public static CentralLib scrap(ChromeDriver driver, String originPaperName, String congressPaperName, String excelAuthor) {
        String paperName = congressPaperName != null ? congressPaperName : originPaperName;
        List<String> queryList = new ArrayList<>();
        if (congressPaperName != null)
            queryList.add(rearrangeAbnormalSubtitle(originPaperName));

        queryList.add(rearrangeAbnormalSubtitle(paperName.trim()));

        String trimmedQuery = queryPretreatment(paperName);
        queryList.add(trimmedQuery);

        addSubtitleAndSubLang(queryList, trimmedQuery);

        List<CentralLib> matchedList = new ArrayList<>();

        for (String query : queryList) {
            try {
                driver.get(CENTRAL_LIB_URL + query);
            } catch (WebDriverException e) {
                e.printStackTrace();
                throw new TimeoutException();
            }

            List<WebElement> noResult = driver.findElementsByCssSelector("div.cont_wrap li.ucsrch9_item.no_cont");

            if (isExist(noResult)) continue;

            List<WebElement> sections = driver.findElementsByCssSelector("div.section_cont_wrap div.section_cont");

            List<WebElement> rows = null;

            for (WebElement section : sections) {
                List<WebElement> h3 = section.findElements(By.ByCssSelector.cssSelector("div.cont_top h3"));
                if (isExist(h3) && h3.get(0).getText().contains("논문"))
                    rows = section.findElements(By.ByCssSelector.cssSelector("div.cont_list div.row"));
            }

            if (rows == null) continue;

            CentralLib centralLib = new CentralLib(ORGAN_NAME);
            centralLib.setQuery(query);

            boolean isDigitalMethodAdded = false;
            String prevFullTitle = null;

            rows = rows.subList(0, Math.min(rows.size(), 2));
            for (WebElement row : rows) {
                WebElement title = row.findElement(By.ByCssSelector.cssSelector("a.btn_layer.detail_btn_layer"));
                title.click();

                WebElement closeBtn = new WebDriverWait(driver, 20)
                        .until(ExpectedConditions.elementToBeClickable(By.cssSelector("div#popDetailView div.layer_popup.detail_layer_popup div.popup_header button.btn_close")));

                String fullName = driver.findElementByCssSelector("div#popDetailView h3.detail_tit").getText();
                fullName = removeBigBrackets(fullName);
                String author = row.findElements(By.cssSelector("span.mr.txt_grey")).get(0).getText();

                double jaccard = getJaccard(query, fullName);
                jaccard = Math.max(jaccard, ScrapUtil.similar(paperName, fullName));

                if (jaccard < JACCARD_STANDARD) {
                    closeBtn.click();
                    continue;
                }

                if (prevFullTitle == null) {
                    prevFullTitle = fullName;
                } else {
                    double rowTitleSimilar = getRowTitleMatchRatio(row, query);
                    if (rowTitleSimilar < 0.70 && getJaccard(prevFullTitle, fullName) < 0.8) {
                        closeBtn.click();
                        continue;
                    }
                }

                List<WebElement> tableInfos = driver.findElementsByCssSelector("div.table div.table_row span.cont");
                String claimCode = tableInfos.size() > 6 ? tableInfos.get(6).getAttribute("innerHTML").trim() : "";

                // TODO: 유사도 구분짓기
                if (centralLib.getClaimCode() != null && !centralLib.getClaimCode().equals("") && !claimCode.equals("")) {
                    closeBtn.click();
                    continue;
                }

                centralLib.setJaccard(jaccard);

                if (!author.trim().equals(excelAuthor.trim())) {
                    centralLib.setAuthorDiff(excelAuthor + " = " + author);
                }

                if (!claimCode.equals("")) {
                    centralLib.setOriginal(true);
                    centralLib.setClaimCode(claimCode);
                    String service = ScrapUtil.removeHtmlComment(tableInfos.get(8).getAttribute("innerHTML").trim());
                    centralLib.setServiceMethod(centralLib.getServiceMethod() == null ? service : centralLib.getServiceMethod() + "/" + service);
                }

                List<WebElement> digitalBtn = row.findElements(By.ByCssSelector.cssSelector("span.row_btn_wrap a"));
                if (isExist(digitalBtn) ) {
                    centralLib.setDigital(true);
                    centralLib.setDigitalUrl(driver.getCurrentUrl());
                    if (!isDigitalMethodAdded) {
                        List<WebElement> infos = row.findElements(By.ByCssSelector.cssSelector("span span.comments.txt_black+*"));
                        String service = getDigitalService(infos);
                        centralLib.setServiceMethod(centralLib.getServiceMethod() == null ? service : service + "/" + centralLib.getServiceMethod());
                        isDigitalMethodAdded = true;
                    }
                }

                closeBtn.click();

                if (!matchedList.contains(centralLib))
                    matchedList.add(centralLib);
            }

            if (matchedList.size() > 0) break;
        }

        if (matchedList.size() == 0) {
            CentralLib noResult = new CentralLib(ORGAN_NAME);
            noResult.setOriginal(null);
            noResult.setDigital(null);
            noResult.setRemark("일치하는 검색 결과 없음");
            noResult.setServiceMethod("서비스 제공 불가");
            return noResult;
        }

        return matchedList.get(0);
    }

    private static double getRowTitleMatchRatio(WebElement row, String query) {
        int matchSize = 0;
        WebElement a = row.findElement(By.ByCssSelector.cssSelector("span.txt_left.row_txt_tit a"));
        int totalSize = query.replace(" ", "").length();

        List<WebElement> searchingText = a.findElements(By.ByCssSelector.cssSelector("span.searching_txt"));
        for (WebElement text : searchingText) {
            String match = text.getText().trim();
            matchSize += match.length();
        }

        return matchSize / (double) totalSize;
    }

    private static String removeSpan(String html) {
        return html.replace("<span class=\"searching_txt\">", "")
                .replace("</span>", "")
                .replace(" ", "");
    }

    private static String getDigitalService(List<WebElement> infos) {
        StringBuilder result = new StringBuilder();

        for (WebElement info : infos) {
            result.append(info.getAttribute("innerHTML").trim()).append(" ");
        }
        return result.toString().trim();
    }

    private static double getJaccard(String query, String text) {
        double result = 0.0;

        double fullMatch = similar(query, text);
        result = Math.max(result, fullMatch);
        if (result > 0.9) return result;

        if (query.contains("=") && text.contains("=")) {
            String[] split = text.split("=");
            double withoutSubLang = similar(query.split("=")[0], split[0]);
            double subLang = similar(query.split("=")[1], split.length > 1 ? split[1] : text);
            result = Math.max(result, Math.max(withoutSubLang, subLang));
            if (result > 0.9) return result;
        }

        if (!query.contains("=") && text.contains("=")) {
            String[] split = text.split("=");
            double withoutSubLang = similar(query, split[0]);
            double subLang = similar(query, split.length > 1 ? split[1] : text);
            result = Math.max(result, Math.max(withoutSubLang, subLang));
            if (result > 0.9) return result;
        }

        if (query.contains("=") && !text.contains("=")) {
            double withoutSubLang = similar(query.split("=")[0], text);
            double subLang = similar(query.split("=")[1], text);
            result = Math.max(result, Math.max(withoutSubLang, subLang));
            if (result > 0.9) return result;
        }

//        if (!query.contains(":") && text.contains(":")) {
//            double withoutSubtitle = jaccard(query, text.split(":")[0]);
//            result = Math.max(result, withoutSubtitle);
//            if (result > 0.9) return result;
//        }

        if (query.contains(":") && !text.contains(":")) {
            double withoutSubtitle = similar(query.split(":")[0], text);
            result = Math.max(result, withoutSubtitle);
            if (result > 0.9) return result;
        }

        return result;
    }

    private static void addSubtitleAndSubLang(List<String> list, String query) {
        if (query.contains("=")) {
            String[] split = query.split("=");
            String originLang = split[0].trim();
            String subLang = split[1].trim();
            list.add(0, originLang);
            list.add(subLang);
            if (originLang.contains(":")) {
                String[] originLangSplit = originLang.split(":");
                list.add(1, originLangSplit[0].trim());
            }
            if (subLang.contains(":")) {
                list.add(subLang.split(":")[0].trim());
            }
        } else {
            if (query.contains(":")) {
                list.add(1, query.split(":")[0].trim());
            }
        }
    }
}
