package util;

import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import kr.co.shineware.nlp.komoran.model.KomoranResult;
import kr.co.shineware.nlp.komoran.model.Token;
import org.openqa.selenium.WebElement;

import java.util.*;
import java.util.stream.Collectors;

public class ScrapUtil {

    public static final Komoran komoran = new Komoran(DEFAULT_MODEL.FULL);

    public static String removeBrackets(String query) {
        return removeStringToString(query, "(", ")");
    }

    public static String removeBigBrackets(String query) {
        return removeStringToString(query, "[", "]");
    }

    public static String removeHtmlComment(String query) {
        return removeStringToString(query, "<!--", "-->");
    }

    public static String removeStringToString(String target, String char1, String char2) {
        StringBuilder builder = new StringBuilder(target);

        while (builder.indexOf(char1) != -1) {
            int start = builder.indexOf(char1);
            int end = builder.indexOf(char2);
            String s = builder.substring(0, start) + builder.substring(end + char2.length(), builder.length());
            builder = new StringBuilder(s);
        }

        return builder.toString().trim();
    }

    public static String removeAllSpecialChar(String query) {
        return query.replaceAll("[^ㄱ-ㅎㅏ-ㅣ가-힣a-zA-Z0-9 \\u2e80-\\u2eff\\u31c0-\\u31ef\\u3200-\\u32ff\\u3400-\\u4dbf\\u4e00-\\u9fbf\\uf900-\\ufaff]", " ");
    }

    public static double similar(String str1, String str2) {
        Set<String> s1 = getKomoranAnalyzedSet(str1.toLowerCase());
        Set<String> s2 = getKomoranAnalyzedSet(str2.toLowerCase());

        int sa = s1.size();
        int sb = s2.size();
        s1.retainAll(s2);
        final int intersection = s1.size();

        return 1.0 / (sa + sb - intersection) * intersection;
    }

    private static Set<String> getKomoranAnalyzedSet(String sentence) {
        KomoranResult analyzeResultList = komoran.analyze(sentence);
        List<Token> tokenList = analyzeResultList.getTokenList();
        return tokenList.stream()
                .map(Token::getMorph)
                .collect(Collectors.toSet());
    }

    public static String rearrangeAbnormalSubtitle(String paperName) {
        if (!paperName.contains("="))
            return paperName;

        String[] names = paperName.split("=");

        if (names[1].chars().filter(ch -> ch == ':').count() < 2)
            return paperName;

        int equalIndex = paperName.indexOf("=");
        int colonIndex = paperName.lastIndexOf(":");

        return paperName.substring(0, equalIndex) +
                paperName.substring(colonIndex) +
                paperName.substring(equalIndex, colonIndex - 1);
    }

    public static boolean isExist(List<WebElement> elements) {
        return !elements.isEmpty();
    }

    public static String getExcelValue(Boolean bool) {
        if (bool == null) return "";
        else return bool ? "O" : "X";
    }

    public static String removeQuotes(String str) {
        return str.replaceAll("['\"‘’”“·《≪≫》]", "");
    }

    public static int countCharInStr(String str, char ch) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == ch)
                count++;
        }
        return count;
    }
}
