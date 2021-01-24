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
import static util.ScrapUtil.similar;

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

    public static Pair<Keris, CommonInfo> scrap(ChromeDriver driver, String originPaperName, String congressPaperName, String excelAuthor) {
        String paperName = congressPaperName != null ? congressPaperName : originPaperName;
        List<String> queryList = createQueryList(paperName, originPaperName);
        List<Pair<Keris, CommonInfo>> matchedList = new ArrayList<>();

        for (String query : queryList) {
            try {
                driver.get(KERIS_URL + ScrapUtil.removeAllSpecialChar(query));
            } catch (WebDriverException e) {
                e.printStackTrace();
                throw new TimeoutException();
            }

            List<WebElement> noResult = driver.findElementsByCssSelector("div.contentInner div.srchResultW.noList");
            if (ScrapUtil.isExist(noResult)) {
                continue;
            }

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

                double similarity = getQuerySimilarity(ScrapUtil.removeQuotes(query), ScrapUtil.removeQuotes(titleElement.getText().trim()));
                if ((isSameAuthor && similarity < JACCARD_STANDARD) || (!isSameAuthor && similarity < 0.7)) {
                    continue;
                }

                Keris keris = new Keris(ORGAN_NAME);
                keris.setQuery(query);
                keris.setSimilarity(similarity);

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
                            new WebDriverWait(driver, 15)
                                    .until(ExpectedConditions.elementToBeClickable(By.ByCssSelector.cssSelector("embed")));
                        } catch (TimeoutException e1) {
                            List<WebElement> iframe = driver.findElementsByCssSelector("iframe#download_frm");
                            List<WebElement> mac = driver.findElementsByCssSelector("body.div-area.mac");
                            if (!ScrapUtil.isExist(iframe) && !ScrapUtil.isExist(mac)) {
                                throw new TimeoutException("Keris PDF Timeout");
                            }
                        } catch (UnhandledAlertException e2) {
                            return Pair.create(getFailObject("원문 제공 불가"), commonInfo);
                        }
                    } else {
                        List<WebElement> exception = driver.findElementsByCssSelector("div.thesisInfo ul li.on span font");
                        if (ScrapUtil.isExist(exception)) {
                            keris.setServiceMethod(exception.get(0).getText());
                        } else {
                            keris.setServiceMethod("이용자 신청 후 이용 가능");
                        }
                    }
                }

                matchedList.add(Pair.create(keris, commonInfo));
            }

            if (matchedList.size() > 0) {
                break;
            }
        }

        if (matchedList.size() == 0) {
            System.out.println("size 0 paperName : " + paperName);
            System.out.println("size 0 queryList : " + Arrays.toString(queryList.toArray()));
            return Pair.create(getFailObject("일치하는 검색 결과 없음"), new CommonInfo());
        }

        matchedList.sort((o1, o2) -> Double.compare(o2.getFirst().getSimilarity(), o1.getFirst().getSimilarity()));
        return matchedList.get(0);
    }

    private static List<String> createQueryList(String paperName, String originPaperName) {
        List<String> queryList = new ArrayList<>();
        if (!paperName.equals(originPaperName)) {
            queryList.add(rearrangeAbnormalSubtitle(originPaperName));
        }

        queryList.add(rearrangeAbnormalSubtitle(paperName.trim()));

        String trimmedQuery = queryPretreatment(paperName);
        queryList.add(trimmedQuery);

        addSubtitleAndSubLang(queryList, trimmedQuery);
        return queryList;
    }

    public static String queryPretreatment(String paperName) {
        return rearrangeAbnormalSubtitle(paperName);
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

    private static double getQuerySimilarity(String query, String text) {
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

        if (!query.contains(":") && text.contains(":")) {
            double withoutSubtitle = similar(query, text.split(":")[0]);
            result = Math.max(result, withoutSubtitle);
            if (result > 0.9) return result;
        }

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
                    setShapeInfo(commonInfo, info);
                } else if (column.contains("학위논문")) {
                    setDegreeInfo(commonInfo, info);
                } else if (column.contains("일반주기")) {
                    setDetailInfo(commonInfo, info);
                }
            } catch (NoSuchElementException ignored) {}
        }
        return commonInfo;
    }

    private static void setDetailInfo(CommonInfo commonInfo, WebElement info) {
        String text = info.findElement(By.ByCssSelector.cssSelector("div p")).getText();
        String[] split = text.split("\n");
        for (String s : split) {
            if (s.contains("지도") && s.split(":").length > 1){
                commonInfo.setProfessor(s.split(":")[1]);
            }
        }
    }

    private static void setDegreeInfo(CommonInfo commonInfo, WebElement info) {
        String text = info.findElement(By.ByCssSelector.cssSelector("div p")).getText();
        String degree = extractDegree(text);
        String major = extractMajor(text);
        commonInfo.setDegree(degree);
        commonInfo.setMajor(major);
    }

    private static void setShapeInfo(CommonInfo commonInfo, WebElement info) {
        String shape = info.findElement(By.ByCssSelector.cssSelector("div p")).getText();
        String page = "";
        String size = "";
        if (shape.contains(";")) {
            String[] shapes = shape.split(";");
            if (shapes.length < 2) {
                if (shapes[0].contains("cm")) {
                    size = shapes[0];
                } else {
                    page = shapes[0];
                }
            } else {
                page = shapes[0].trim();
                if (!page.equals("") && !page.contains("p")) {
                    page += " p.";
                }
                size = shapes[1];
            }
        } else if (shape.contains("p.")) {
            String[] shapes = shape.split("p.");
            page = shapes[0].trim() + " p.";
            size = shapes[1].trim();
        }

        commonInfo.setPage(page);
        commonInfo.setSize(size);
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

    private static Keris getFailObject(String remark) {
        Keris keris = new Keris(ORGAN_NAME);
        keris.setServiceMethod("서비스 제공 불가");
        keris.setRemark(remark);
        keris.setDigital(null);
        return keris;
    }
}
