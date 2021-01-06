package scraper;

import dto.CommonInfo;
import dto.Keris;
import org.apache.commons.math3.util.Pair;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import util.ScrapUtil;
import util.Selenium;

import java.util.*;
import java.util.stream.Collectors;

import static util.ScrapUtil.rearrangeAbnormalSubtitle;
import static util.ScrapUtil.removeBrackets;

public class KerisScraper {

    public static final String KERIS_URL = "http://www.riss.kr/search/Search.do?isDetailSearch=N&searchGubun=true" +
            "&viewYn=OP&queryText=&strQuery=&exQuery=&exQueryText=&order=%2FDESC&onHanja=false&strSort=RANK" +
            "&p_year1=&p_year2=&iStartCount=0&orderBy=&mat_type=&mat_subtype=&fulltext_kind=&t_gubun=&learning_type=" +
            "&ccl_code=&inside_outside=&fric_yn=&image_yn=&gubun=&kdc=&ttsUseYn=&fsearchMethod=search&sflag=1" +
            "&isFDetailSearch=N&pageNumber=1&resultKeyword=&fsearchSort=&fsearchOrder=&limiterList=&limiterListText=" +
            "&facetList=&facetListText=&fsearchDB=&icate=bib_t&colName=bib_t&pageScale=10&isTab=Y&regnm=&dorg_storage=" +
            "&language=&language_code=&clickKeyword=&relationKeyword=&query=";

    public static final String ORGAN_NAME = "KERIS";
    public static final double JACCARD_STANDARD = 0.60;

    public static String queryPretreatment(String paperName) {
        String query = rearrangeAbnormalSubtitle(paperName);
        query = removeBrackets(query);
        query = ScrapUtil.removeStringToString(query, "≪", "≫");
        query = query.replaceAll("[,?&-]", "");
        return query;
    }

    public static Pair<Keris, CommonInfo> scrap(ChromeDriver driver, String paperName, String college) {
        Queue<String> queryQueue = new ArrayDeque<>();
        queryQueue.add(paperName.trim());

        String trimmedQuery = queryPretreatment(paperName);
        queryQueue.add(trimmedQuery);

        addSubtitleAndSubLang(queryQueue, trimmedQuery);

        List<Pair<Keris, CommonInfo>> matchedList = new ArrayList<>();

        while (!queryQueue.isEmpty()) {
            String query = queryQueue.poll();

            try {
                driver.get(KERIS_URL + query.replace(":", ""));
            } catch (WebDriverException e) {
                Keris keris = new Keris(ORGAN_NAME);
                keris.setRemark("크롤링 라이브러리 연결 실패");
                return Pair.create(keris, new CommonInfo());
            }

            List<WebElement> noResult = driver.findElementsByCssSelector("div.contentInner div.srchResultW.noList");

            if (ScrapUtil.isExist(noResult)) continue;

            List<WebElement> rows = driver.findElementsByCssSelector("div.contentInner div.srchResultListW div.cont");
            rows = rows.subList(0, Math.min(rows.size(), 1));

            for (WebElement row : rows) {
                WebElement title = row.findElement(By.ByCssSelector.cssSelector("p.title a"));
                title.click();

                // 대학, 유사도 검사
                WebElement publishElement = new WebDriverWait(driver, 30)
                        .until(ExpectedConditions.elementToBeClickable(By.ByCssSelector.cssSelector("div.thesisInfo div.infoDetailL ul li:nth-child(2) div p")));

                if (!isSameCollege(college, publishElement)) continue;

                WebElement titleElement = driver.findElementByCssSelector("div.thesisInfo div.thesisInfoTop h3");

                double jaccard = getJaccard(query, titleElement);
                if (jaccard < JACCARD_STANDARD) continue;

                Keris keris = new Keris(ORGAN_NAME);
                keris.setQuery(query);
                keris.setJaccard(jaccard);

                List<WebElement> btns = driver.findElementsByCssSelector("div.thesisInfo div.btnBunchL ul li:nth-child(1) a");

                if (ScrapUtil.isExist(btns)) {
                    if (btns.get(0).getText().contains("원문")) {
                        WebElement hiddenForm = driver.findElementByCssSelector("form#f");

//                        keris.setDigital(true);
                        keris.setServiceMethod("원문을 url 연계하여 제공");
                        keris.setDigitalUrl(digitalUrl(hiddenForm));
                    } else {
                        keris.setDigital(false);

                        List<WebElement> exception = driver.findElementsByCssSelector("div.thesisInfo ul li.on span font");
                        if (ScrapUtil.isExist(exception))
                            keris.setServiceMethod(exception.get(0).getText());
                    }
                } else {
                    keris.setDigital(false);
                }

                List<WebElement> infos = driver.findElementsByCssSelector("div.infoDetailL ul li");
                CommonInfo commonInfo = getCommonInfo(infos);

                matchedList.add(Pair.create(keris, commonInfo));
            }

            if (matchedList.size() > 0) break;
        }

        if (matchedList.size() == 0)
            return Pair.create(getFailObject(), new CommonInfo());

        matchedList.sort((o1, o2) -> Double.compare(o2.getFirst().getJaccard(), o1.getFirst().getJaccard()));
        return matchedList.get(0);
    }

    private static String digitalUrl(WebElement form) {
        String baseUrl = "http://www.riss.kr/search/download/FullTextDownload.do?";
        List<String> params = List.of(
                "control_no", "p_mat_type", "p_submat_type", "fulltext_kind", "t_gubun", "convertFlag", "naverYN",
                "outLink", "nationalLibraryLocalBibno", "searchGubun", "colName", "DDODFlag", "loginFlag", "url_type", "query"
        );
        String urlParam = params.stream()
                .map((param) ->
                        param + "=" + form.findElement(By.ByCssSelector.cssSelector(String.format("input[name='%s']", param)))
                                .getAttribute("value"))
                .collect(Collectors.joining("&"));

        return baseUrl + urlParam;
    }

    private static double getJaccard(String query, WebElement titleElement) {
        String text = titleElement.getText().trim();

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

    private static boolean isSameCollege(String college, WebElement publishInfo) {
        String text = publishInfo.getText();
        String[] split = text.split(":");
        String[] split1 = split[1].split(",");
        return ScrapUtil.jaccard(college, split1[0].trim()) > 0.8;
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

    private static CommonInfo getCommonInfo(List<WebElement> infos) {
        CommonInfo commonInfo = new CommonInfo();
        for (WebElement info : infos) {
            try {
                String column = info.findElement(By.ByCssSelector.cssSelector("span.strong")).getAttribute("innerHTML");
                if (column.contains("형태")) {
                    String shape = info.findElement(By.ByCssSelector.cssSelector("div p")).getText();
                    String[] shapes = shape.split(";");
                    if (shapes.length < 2) {
                        if (shapes[0].contains("cm")) {
                            commonInfo.setSize(shapes[0]);
                        } else {
                            commonInfo.setPage(shapes[0]);
                        }
                    } else {
                        commonInfo.setPage(shapes[0]);
                        commonInfo.setSize(shapes[1]);
                    }
                } else if (column.contains("학위논문")) {
                    String text = info.findElement(By.ByCssSelector.cssSelector("div p")).getText();
                    String degree = extractDegree(text);
                    String major = extractMajor(text);
                    commonInfo.setDegree(degree);
                    commonInfo.setMajor(major);
                } else if (column.contains("일반주기")) {
                    String text = info.findElement(By.ByCssSelector.cssSelector("div p")).getText();
                    String[] split = text.split("\n");
                    for (String s : split) {
                        if (s.contains("지도") && s.split(":").length > 1)
                            commonInfo.setProfessor(s.split(":")[1]);
                    }
                }
            } catch (NoSuchElementException ignored) {}
        }
        return commonInfo;
    }

    private static String extractMajor(String text) {
        if (!text.contains("--")) return "";
        String[] split = text.split("--");
        String data = split[1];
        if (!data.contains(":")) return "";
        String[] split2 = data.split(":");
        String[] split3 = split2[1].trim().split(" ");
        return  split3[0].trim();
    }

    private static String extractDegree(String text) {
        if (!text.contains("--")) return "";
        String[] split = text.split("--");
        String data = split[0];
        if (!data.contains("(")) return "";
        int start = data.indexOf("(");
        int end = data.indexOf(")");
        return data.substring(start + 1, end);
    }

    private static Keris getFailObject() {
        Keris keris = new Keris(ORGAN_NAME);
        keris.setServiceMethod("서비스 제공 불가");
        keris.setRemark("일치하는 검색 결과 없음");
        keris.setDigital(null);
        return keris;
    }
}
