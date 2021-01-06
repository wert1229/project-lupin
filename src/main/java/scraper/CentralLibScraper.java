package scraper;

import dto.CentralLib;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import util.ScrapUtil;
import util.Selenium;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static util.ScrapUtil.*;

public class CentralLibScraper {

    public static final String CENTRAL_LIB_URL = "https://www.nl.go.kr/NL/contents/search.do?pageNum=1&pageSize=4&srchTarget=total&kwd=";

    public static final String ORGAN_NAME = "국립중앙도서관";
    public static final double JACCARD_STANDARD = 0.60;

    public static String queryPretreatment(String paperName) {
        String query = rearrangeAbnormalSubtitle(paperName);
        query = removeBrackets(query);
        query = ScrapUtil.removeStringToString(query, "<<", ">>");
        query = query.replace("'", " ").replaceAll("[?]", "");
        return query;
    }

    public static CentralLib scrap(ChromeDriver driver, String paperName) {
        Queue<String> queryQueue = new ArrayDeque<>();
        queryQueue.add(paperName.trim());

        String trimmedQuery = queryPretreatment(paperName);
        queryQueue.add(trimmedQuery);

        addSubtitleAndSubLang(queryQueue, trimmedQuery);

        List<CentralLib> matchedList = new ArrayList<>();

        while (!queryQueue.isEmpty()) {
            String query = queryQueue.poll();

            try {
                driver.get(CENTRAL_LIB_URL + query);
            } catch (WebDriverException e) {
                CentralLib centralLib = new CentralLib(ORGAN_NAME);
                centralLib.setRemark("크롤링 라이브러리 연결 실패");
                return centralLib;
            }

//            WebElement searchText = driver.findElementByCssSelector("input#main_input-text");
//            searchText.clear();
//            searchText.sendKeys(query);
//
//            WebElement searchBtn = driver.findElementByCssSelector("button.btn-search");
//            searchBtn.click();

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

            rows = rows.subList(0, Math.min(rows.size(), 2));
            for (WebElement row : rows) {
                WebElement title = row.findElement(By.ByCssSelector.cssSelector("a.btn_layer.detail_btn_layer"));
                title.click();

                String fullName = new WebDriverWait(driver, 60)
                        .until(ExpectedConditions.elementToBeClickable(By.cssSelector("div#popDetailView h3.detail_tit"))).getText();

                WebElement closeBtn = driver.findElementByCssSelector("div#popDetailView div.layer_popup.detail_layer_popup div.popup_header button.btn_close");

//                String fullName = driver.findElementByCssSelector("div#popDetailView h3.detail_tit").getText();
                fullName = removeBigBrackets(fullName);

                double jaccard = getJaccard(query, fullName);
                jaccard = Math.max(jaccard, ScrapUtil.jaccard(paperName, fullName));

                if (jaccard < JACCARD_STANDARD) {
                    closeBtn.click();
                    continue;
                }
                centralLib.setJaccard(jaccard);

                List<WebElement> tableInfos = driver.findElementsByCssSelector("div.table div.table_row span.cont");
                String claimCode = tableInfos.size() > 6 ? tableInfos.get(6).getAttribute("innerHTML").trim() : "";

                if (!claimCode.equals("")) {
                    centralLib.setOriginal(true);
                    centralLib.setClaimCode(claimCode);
                    String service = ScrapUtil.removeHtmlComment(tableInfos.get(8).getAttribute("innerHTML").trim());
                    centralLib.setServiceMethod(centralLib.getServiceMethod() == null ? service : centralLib.getServiceMethod() + "/" + service);
                }

                List<WebElement> digitalBtn = row.findElements(By.ByCssSelector.cssSelector("span.row_btn_wrap a"));

                if (isExist(digitalBtn) && digitalBtn.get(0).getText().contains("온라인")) {
                    centralLib.setDigital(true);
                    centralLib.setDigitalUrl(driver.getCurrentUrl());
                    List<WebElement> infos = row.findElements(By.ByCssSelector.cssSelector("div.row span"));
                    String service = getDigitalService(infos);
                    centralLib.setServiceMethod(centralLib.getServiceMethod() == null ? service : service  + "/" + centralLib.getServiceMethod());
                }

                if (!matchedList.contains(centralLib))
                    matchedList.add(centralLib);

                closeBtn.click();
            }


            if (matchedList.size() > 0) break;
        }

        if (matchedList.size() == 0) {
            CentralLib noResult = new CentralLib(ORGAN_NAME);
            noResult.setRemark("일치하는 검색 결과 없음");
            noResult.setServiceMethod("서비스 이용 불가");
            return noResult;
        }

        return matchedList.get(0);
    }

    private static String getDigitalService(List<WebElement> infos) {
        for (WebElement info : infos) {
            List<WebElement> span = info.findElements(By.ByCssSelector.cssSelector("span.comments.txt_black"));
            if (isExist(span) && span.get(0).getAttribute("innerHTML").contains("원문")) {
                WebElement service = info.findElement(By.ByCssSelector.cssSelector("span span:last-child"));
                return service.getAttribute("innerHTML").trim();
            }
        }
        return "";
    }

    private static double getJaccard(String query, String text) {
        double fullMatch = jaccard(query, text);
        if (fullMatch > JACCARD_STANDARD) return fullMatch;

        if (!query.contains("=") && text.contains("=")) {
            double withoutSubLang = jaccard(query, text.split("=")[0]);
            if (withoutSubLang > JACCARD_STANDARD) return withoutSubLang;
        }

        if (!query.contains("=") && text.contains("=")) {
            double subLang = jaccard(query, text.split("=")[1]);
            if (subLang > JACCARD_STANDARD) return subLang;
        }

        if (!query.contains(":") && text.contains(":")) {
            double withoutSubtitle = jaccard(query, text.split(":")[0]);
            if (withoutSubtitle > JACCARD_STANDARD) return withoutSubtitle;
        }

        return 0;
    }

    private static void addSubtitleAndSubLang(Queue<String> queue, String query) {
        if (query.contains("=")) {
            String[] split = query.split("=");
            String originLang = split[0].trim();
            String subLang = split[1].trim();
            queue.add(originLang);
            queue.add(subLang);
            if (originLang.contains(":"))
                queue.add(originLang.split(":")[0].trim());
            if (subLang.contains(":"))
                queue.add(originLang.split(":")[0].trim());
        } else {
            if (query.contains(":"))
                queue.add(query.split(":")[0].trim());
        }
    }
}
