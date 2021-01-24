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

import java.util.ArrayList;
import java.util.List;

import static util.ScrapUtil.rearrangeAbnormalSubtitle;
import static util.ScrapUtil.removeBrackets;

public class CongressScraper {

//    public static final String CONGRESS_URL = "https://dl.nanet.go.kr/search/searchInnerList.do";
    public static final String CONGRESS_URL = "https://dl.nanet.go.kr/";
    public static final String ORGAN_NAME = "국회도서관";
    public static final double JACCARD_STANDARD = 0.60;

    public static String queryPretreatment(String paperName) {
        String query = rearrangeAbnormalSubtitle(paperName);
        query = removeBrackets(query);
        query = ScrapUtil.removeStringToString(query, "≪", "≫");
        query = query.replaceAll("[,?&-]", "");
        return query;
    }

    public static Pair<Congress, CommonInfo> scrap(ChromeDriver driver, String controlCode, String paperName) {
        if (controlCode == null || controlCode.equals("")) return null;
        List<String> queryList = new ArrayList<>();
        queryList.add(controlCode);
        queryList.add(paperName.trim());

        String trimmedQuery = queryPretreatment(paperName);
        queryList.add(trimmedQuery);

        addSubtitleAndSubLang(queryList, trimmedQuery);

        List<Pair<Congress, CommonInfo>> matchedList = new ArrayList<>();

        for (String query : queryList) {
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
//            WebElement searchBtn = driver.findElementByCssSelector("input#searchBtn");
            WebElement searchBtn = driver.findElementByCssSelector("input[type='submit']");
            searchBtn.click();

            if (ScrapUtil.isExist(driver.findElementsByCssSelector("li.none")))
                continue;

            String type = new WebDriverWait(driver, 3)
                    .until(ExpectedConditions.elementToBeClickable(By.cssSelector("ul.list li:first-child ul li:first-child"))).getText();

            if (!type.contains("논문"))
                continue;

            new WebDriverWait(driver, 3)
                    .until(ExpectedConditions.elementToBeClickable(By.cssSelector("ul.list li:nth-child(1) a"))).click();

            WebElement titleSpan = driver.findElementByCssSelector("div.searchDetail div.detailContent dl#DP_TITLE_FULL dd span.iBold");

            if (!query.equals(controlCode)) {
                double jaccard = getJaccard(query, titleSpan);
                if (jaccard < JACCARD_STANDARD) continue;
            }

            Congress congress = new Congress(ORGAN_NAME);
            congress.setControlCode(controlCode);

            if (ScrapUtil.isExist(driver.findElementsByCssSelector("div.detailContent a[onclick].pdf10"))
                    || ScrapUtil.isExist(driver.findElementsByCssSelector("div.detailContent a.pdf13.readonly"))) {
                congress.setDigital(true);
                congress.setDigitalUrl(driver.getCurrentUrl());
            } else {
                congress.setDigital(false);
            }

            List<WebElement> location = driver.findElementsByCssSelector("div.searchDetail div.detailContent dl#DP_LOCATION dd");

            if (ScrapUtil.isExist(location)) {
                congress.setOriginal(!location.get(0).getText().contains("전자"));
                congress.setServiceMethod(location.get(0).getText());
            } else {
                congress.setOriginal(false);
                congress.setServiceMethod("전자자료");
            }

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
            notExist.setDigital(null);
            notExist.setOriginal(null);
            return Pair.create(notExist, new CommonInfo());
        }

        return matchedList.get(0);
    }

    private static void addSubtitleAndSubLang(List<String> list, String query) {
        if (query.contains("=")) {
            String[] split = query.split("=");
            String originLang = split[0].trim();
            String subLang = split[1].trim();
            list.add(originLang);
            list.add(subLang);
            if (originLang.contains(":"))
                list.add(originLang.split(":")[0].trim());
            if (subLang.contains(":"))
                list.add(originLang.split(":")[0].trim());
        } else {
            if (query.contains(":"))
                list.add(query.split(":")[0].trim());
        }
    }

    private static String getProfessor(WebElement dd) {
        String text = dd.getText().trim();
        if (text.contains("\n")) text = text.split("\n")[0];
        String[] split = text.split(":");
        return split.length > 1 ? split[1].trim() : "";
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
        String fullTitle = span.getText();
        if (fullTitle.contains("</a>")) fullTitle = ScrapUtil.removeStringToString(fullTitle, "<a", "</a>");
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
        String text = titleElement.getText();
        text = text.substring(0, text.lastIndexOf("/"));

        double fullMatch = ScrapUtil.similar(query, text);
        if (fullMatch > JACCARD_STANDARD) return fullMatch;

        if (!query.contains("=") && text.contains("=")) {
            double withoutSubLang = ScrapUtil.similar(query, text.split("=")[0]);
            if (withoutSubLang > JACCARD_STANDARD) return withoutSubLang;
        }

        if (!query.contains("=") && text.contains("=")) {
            double subLang = ScrapUtil.similar(query, text.split("=")[1]);
            if (subLang > JACCARD_STANDARD) return subLang;
        }

        if (!query.contains(":") && text.contains(":")) {
            double withoutSubtitle = ScrapUtil.similar(query, text.split(":")[0]);
            if (withoutSubtitle > JACCARD_STANDARD) return withoutSubtitle;
        }

        return 0;
    }
}
