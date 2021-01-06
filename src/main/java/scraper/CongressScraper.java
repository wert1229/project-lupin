package scraper;

import dto.CommonInfo;
import dto.Congress;
import org.apache.commons.math3.util.Pair;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import util.ScrapUtil;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static util.ScrapUtil.rearrangeAbnormalSubtitle;
import static util.ScrapUtil.removeBrackets;

public class CongressScraper {

    public static final String CONGRESS_URL = "https://dl.nanet.go.kr/search/searchInnerList.do";
    public static final String ORGAN_NAME = "국회도서관";
    public static final double JACCARD_STANDARD = 0.60;

    public static String queryPretreatment(String paperName) {
        String query = rearrangeAbnormalSubtitle(paperName);
        query = removeBrackets(query);
        query = ScrapUtil.removeStringToString(query, "<<", ">>");
        query = query.replaceAll("[,?&-]", "");
        return query;
    }

    public static Pair<Congress, CommonInfo> scrap(ChromeDriver driver, String controlCode, String paperName) {
        if (controlCode == null || controlCode.equals("")) return null;
        Queue<String> queryQueue = new ArrayDeque<>();
        queryQueue.add(controlCode);
        queryQueue.add(paperName.trim());

        String trimmedQuery = queryPretreatment(paperName);
        queryQueue.add(trimmedQuery);

        addSubtitleAndSubLang(queryQueue, trimmedQuery);

        List<Pair<Congress, CommonInfo>> matchedList = new ArrayList<>();

        while (!queryQueue.isEmpty()) {
            String query = queryQueue.poll();

            try {
                driver.get(CONGRESS_URL);
            } catch (WebDriverException e) {
                Congress timeout = new Congress(ORGAN_NAME);
                timeout.setControlCode(controlCode);
                timeout.setRemark("크롤링 라이브러리 연결 실패");
                return Pair.create(timeout, new CommonInfo());
            }

            WebElement searchInput = driver.findElementByCssSelector("input#searchQuery");
            searchInput.sendKeys(query);
            WebElement searchBtn = driver.findElementByCssSelector("input#searchBtn");
            searchBtn.click();

            if (ScrapUtil.isExist(driver.findElementsByCssSelector("li.none"))) continue;

            new WebDriverWait(driver, 2)
                    .until(ExpectedConditions.elementToBeClickable(By.cssSelector("ul.list li:nth-child(1) a"))).click();

            WebElement titleSpan = driver.findElementByCssSelector("div.searchDetail div.detailContent dl#DP_TITLE_FULL dd span.iBold");
            double jaccard = getJaccard(query, titleSpan);
            if (jaccard < JACCARD_STANDARD) continue;

            Congress congress = new Congress(ORGAN_NAME);
            congress.setControlCode(controlCode);

            if (ScrapUtil.isExist(driver.findElementsByCssSelector("div.detailContent a[onclick].pdf10"))
                    || ScrapUtil.isExist(driver.findElementsByCssSelector("div.detailContent a.pdf13.readonly"))) {
                congress.setDigital(true);
                congress.setDigitalUrl(driver.getCurrentUrl());
            } else {
                congress.setDigital(false);
            }

            WebElement location = driver.findElementByCssSelector("div.searchDetail div.detailContent dl#DP_LOCATION dd");
            congress.setOriginal(!location.getText().contains("전자"));
            congress.setServiceMethod(location.getText());

            if (isNeedMOU(driver)) {
                congress.setServiceMethod("협정기관 이용자 서비스 이용 가능/" + congress.getServiceMethod());
            }

            CommonInfo commonInfo = new CommonInfo();

            WebElement fullTitle = driver.findElementByCssSelector("div.searchDetail div.detailContent dl#DP_TITLE_FULL dd span");
            String title = trimFullTitle(fullTitle);
            commonInfo.setFullName(title);

            WebElement form = driver.findElementByCssSelector("div.searchDetail div.detailContent dl#DETAIL_FORM dd");
            String[] formInfo = getForm(form);
            commonInfo.setPage(formInfo[0]);
            commonInfo.setSize(formInfo[1]);

            WebElement remark = driver.findElementByCssSelector("div.searchDetail div.detailContent dl#DETAIL_REMARK dd");
            commonInfo.setDegree(getDegree(remark));
            commonInfo.setMajor(getMajor(remark));
            commonInfo.setProfessor(getProfessor(remark));

            matchedList.add(Pair.create(congress, commonInfo));

            if (matchedList.size() > 0) break;
        }

        if (matchedList.size() == 0) {
            Congress notExist = new Congress(ORGAN_NAME);
            notExist.setServiceMethod("서비스 제공 불가");
            notExist.setControlCode(controlCode);
            notExist.setRemark("검색 결과 없음");
            return Pair.create(notExist, new CommonInfo());
        }

        return matchedList.get(0);
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

    private static String getProfessor(WebElement dd) {
        String text = dd.getText().trim();
        String[] split = text.split(":");
        return split[1].trim();
    }

    private static String getMajor(WebElement dd) {
        String text = dd.getText().trim();
        String[] split = text.split("--");
        String subtext = split[1].trim();
        String[] split1 = subtext.split(",");
        return split1[1].trim();
    }

    private static String getDegree(WebElement dd) {
        String text = dd.getText().trim();
        String[] split = text.split("--");
        String degree = split[0];
        int start = degree.indexOf("(");
        int end = degree.indexOf(")");
        return degree.substring(start + 1, end);
    }

    private static String[] getForm(WebElement dd) {
        String text = dd.getText().trim();
        if (text.contains(";")) return text.split(";");
        return List.of("", "").toArray(new String[2]);
    }

    private static String trimFullTitle(WebElement span) {
        String fullTitle = span.getAttribute("innerHTML");
        if (fullTitle.contains("/")) fullTitle = fullTitle.substring(0, fullTitle.lastIndexOf("/"));
        if (fullTitle.contains("[")) fullTitle = ScrapUtil.removeBigBrackets(fullTitle);
        return fullTitle.trim();
    }

    private static boolean isNeedMOU(ChromeDriver driver) {
        List<WebElement> links = driver.findElementsByCssSelector("div.detailContent a.iBlue");
        for (WebElement link : links) {
            if (link.getText().contains("협정")) return true;
        }
        return false;
    }

    private static double getJaccard(String query, WebElement titleElement) {
        String text = titleElement.getAttribute("innerHTML");
        text = text.substring(0, text.lastIndexOf("/"));

        double fullMatch = ScrapUtil.jaccard(query, text);
        if (fullMatch > JACCARD_STANDARD) return fullMatch;

        if (!query.contains("=") && text.contains("=")) {
            double withoutSubLang = ScrapUtil.jaccard(query, text.split("=")[0]);
            if (withoutSubLang > JACCARD_STANDARD) return withoutSubLang;
        }

        if (!query.contains("=") && text.contains("=")) {
            double subLang = ScrapUtil.jaccard(query, text.split("=")[1]);
            if (subLang > JACCARD_STANDARD) return subLang;
        }

        if (!query.contains(":") && text.contains(":")) {
            double withoutSubtitle = ScrapUtil.jaccard(query, text.split(":")[0]);
            if (withoutSubtitle > JACCARD_STANDARD) return withoutSubtitle;
        }

        return 0;
    }
}
