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

import java.util.*;
import java.util.stream.Collectors;

import static util.ScrapUtil.*;
import static util.ScrapUtil.jaccard;

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
        return rearrangeAbnormalSubtitle(paperName);
    }

    public static Pair<Keris, CommonInfo> scrap(ChromeDriver driver, String originPaperName, String congressPaperName, String excelAuthor) {
        String paperName = congressPaperName != null ? congressPaperName : originPaperName;
        List<String> queryList = new ArrayList<>();
        if (congressPaperName != null)
            queryList.add(rearrangeAbnormalSubtitle(originPaperName));

        queryList.add(rearrangeAbnormalSubtitle(paperName.trim()));

        String trimmedQuery = queryPretreatment(paperName);
        queryList.add(trimmedQuery);

        addSubtitleAndSubLang(queryList, trimmedQuery);

        List<Pair<Keris, CommonInfo>> matchedList = new ArrayList<>();

        for (String query : queryList) {
            try {
                driver.get(KERIS_URL + ScrapUtil.removeAllSpecialChar(query));
            } catch (WebDriverException e) {
                Keris keris = new Keris(ORGAN_NAME);
                keris.setRemark("크롤링 라이브러리 연결 실패");
                return Pair.create(keris, new CommonInfo());
            }

            List<WebElement> noResult = driver.findElementsByCssSelector("div.contentInner div.srchResultW.noList");

            if (ScrapUtil.isExist(noResult)) continue;

            List<WebElement> rows = driver.findElementsByCssSelector("div.contentInner div.srchResultListW div.cont");
            rows = rows.subList(0, Math.min(rows.size(), 3));

            List<String> rowLinks = new ArrayList<>();
            for (WebElement row : rows) {
                WebElement title = row.findElement(By.ByCssSelector.cssSelector("p.title a"));
                String href = title.getAttribute("href");
                rowLinks.add(href);
            }

            for (String link : rowLinks) {
                driver.navigate().to(link);

                // 저자, 제목 유사도 검사
                WebElement authorElement = new WebDriverWait(driver, 20)
                            .until(ExpectedConditions.elementToBeClickable(By.ByCssSelector.cssSelector("div.thesisInfo div.infoDetailL ul li:nth-child(1) div p .instituteInfo")));
                String author = authorElement.getText().trim();
                boolean isSameAuthor = excelAuthor.trim().equalsIgnoreCase(author);

                WebElement titleElement = driver.findElementByCssSelector("div.thesisInfo div.thesisInfoTop h3");

                double jaccard = getJaccard(ScrapUtil.removeQuotes(query), ScrapUtil.removeQuotes(titleElement.getText().trim()));
                if ((isSameAuthor && jaccard < JACCARD_STANDARD) || (!isSameAuthor && jaccard < 0.7)) continue;

                Keris keris = new Keris(ORGAN_NAME);
                keris.setQuery(query);
                keris.setJaccard(jaccard);

                if (!isSameAuthor) {
                    keris.setAuthorDiff(excelAuthor + " = " + author);
                }

                List<WebElement> infos = driver.findElementsByCssSelector("div.infoDetailL ul li");
                CommonInfo commonInfo = getCommonInfo(infos);

                List<WebElement> btns = driver.findElementsByCssSelector("div.thesisInfo div.btnBunchL ul li:nth-child(1) a");

                if (ScrapUtil.isExist(btns)) {
                    if (btns.get(0).getText().contains("원문")) {
                        WebElement hiddenForm = driver.findElementByCssSelector("form#f");
                        String digitalUrl = digitalUrl(hiddenForm);

                        keris.setServiceMethod("원문을 url 연계하여 제공");
                        keris.setDigitalUrl(digitalUrl);

                        driver.navigate().to(digitalUrl);
                        try {
                            WebElement formElement = new WebDriverWait(driver, 10)
                                    .until(ExpectedConditions.elementToBeClickable(By.ByCssSelector.cssSelector("form#orgViewForm")));
                            WebElement input = formElement.findElement(By.cssSelector("input[name='fileRealName']"));
                            String pdfName = input.getAttribute("value");
                            keris.setFileName(pdfName);
                        } catch (TimeoutException e1) {
                            List<WebElement> iframe = driver.findElementsByCssSelector("iframe#download_frm");
                            if (ScrapUtil.isExist(iframe)) {
                                driver.switchTo().frame("download_frm");
                                WebElement button = driver.findElementByCssSelector("div#main-content a");
                                button.click();
                                String href = button.getAttribute("href");
                                String pdfName = href.substring(href.lastIndexOf("/") + 1);
                                keris.setFileName(pdfName);
                            } else {
                                try {
                                    driver.switchTo().alert();
                                    return Pair.create(getFailObject(), commonInfo);
                                } catch (NoAlertPresentException e2) {
                                    throw new TimeoutException("Keris PDF Download Timeout");
                                }
                            }
                        }
                    } else {
                        keris.setDigital(false);

                        List<WebElement> exception = driver.findElementsByCssSelector("div.thesisInfo ul li.on span font");
                        if (ScrapUtil.isExist(exception))
                            keris.setServiceMethod(exception.get(0).getText());
                        else
                            keris.setServiceMethod("이용자 신청 후 이용 가능");
                    }
                } else {
                    keris.setDigital(false);
                }

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

    private static double getJaccard(String query, String text) {
        double result = 0.0;

        double fullMatch = jaccard(query, text);
        result = Math.max(result, fullMatch);
        if (result > 0.9) return result;

        if (query.contains("=") && text.contains("=")) {
            String[] split = text.split("=");
            double withoutSubLang = jaccard(query.split("=")[0], split[0]);
            double subLang = jaccard(query.split("=")[1], split.length > 1 ? split[1] : text);
            result = Math.max(result, Math.max(withoutSubLang, subLang));
            if (result > 0.9) return result;
        }

        if (!query.contains("=") && text.contains("=")) {
            String[] split = text.split("=");
            double withoutSubLang = jaccard(query, split[0]);
            double subLang = jaccard(query, split.length > 1 ? split[1] : text);
            result = Math.max(result, Math.max(withoutSubLang, subLang));
            if (result > 0.9) return result;
        }

        if (query.contains("=") && !text.contains("=")) {
            double withoutSubLang = jaccard(query.split("=")[0], text);
            double subLang = jaccard(query.split("=")[1], text);
            result = Math.max(result, Math.max(withoutSubLang, subLang));
            if (result > 0.9) return result;
        }

        if (!query.contains(":") && text.contains(":")) {
            double withoutSubtitle = jaccard(query, text.split(":")[0]);
            result = Math.max(result, withoutSubtitle);
            if (result > 0.9) return result;
        }

        if (query.contains(":") && !text.contains(":")) {
            double withoutSubtitle = jaccard(query.split(":")[0], text);
            result = Math.max(result, withoutSubtitle);
            if (result > 0.9) return result;
        }

        return result;
    }

    private static boolean isSameAuthor(String author, WebElement publishInfo) {
        String text = publishInfo.getText().trim();
        return text.equalsIgnoreCase(author.trim());
    }

    private static void addSubtitleAndSubLang(List<String> list, String query) {
        if (query.contains("=")) {
            String[] split = query.split("=");
            String originLang = split[0].trim();
            String subLang = split[1].trim();
            list.add(0, originLang);
            list.add(subLang);
            if (originLang.contains(":"))
                list.add(1, originLang.split(":")[0].trim());
            if (subLang.contains(":"))
                list.add(subLang.split(":")[0].trim());
        } else {
            if (query.contains(":"))
                list.add(query.split(":")[0].trim());
        }
    }

    private static CommonInfo getCommonInfo(List<WebElement> infos) {
        CommonInfo commonInfo = new CommonInfo();
        for (WebElement info : infos) {
            try {
                String column = info.findElement(By.ByCssSelector.cssSelector("span.strong")).getAttribute("innerHTML");
                if (column.contains("형태")) {
                    String shape = info.findElement(By.ByCssSelector.cssSelector("div p")).getText();
                    if (shape.contains(";")) {
                        String[] shapes = shape.split(";");
                        if (shapes.length < 2) {
                            if (shapes[0].contains("cm")) {
                                commonInfo.setSize(shapes[0]);
                            } else {
                                commonInfo.setPage(shapes[0]);
                            }
                        } else {
                            String page = shapes[0].trim();
                            if (!page.equals("") && !page.contains("p"))
                                page += " p.";
                            commonInfo.setPage(page);
                            commonInfo.setSize(shapes[1]);
                        }
                    } else if (shape.contains("p.")) {
                        String[] shapes = shape.split("p.");
                        commonInfo.setPage(shapes[0].trim() + " p.");
                        commonInfo.setSize(shapes[1].trim());
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
        if (data.contains(":")) {
            String[] split2 = data.split(":");
            String[] split3 = split2[1].trim().split(" ");
            return split3[0].trim();
        } else {
            return data;
        }
    }

    private static String extractDegree(String text) {
        if (!text.contains("--")) return "";
        String[] split = text.split("--");
        String data = split[0];
        if (data.contains("(")) {
            int start = data.indexOf("(");
            int end = data.indexOf(")");
            return data.substring(start + 1, end);
        } else if (data.contains("[")) {
            int start = data.indexOf("[");
            int end = data.indexOf("]");
            return data.substring(start + 1, end);
        } else {
            return "";
        }

    }

    private static Keris getFailObject() {
        Keris keris = new Keris(ORGAN_NAME);
        keris.setServiceMethod("서비스 제공 불가");
        keris.setRemark("일치하는 검색 결과 없음");
        keris.setDigital(null);
        return keris;
    }
}
