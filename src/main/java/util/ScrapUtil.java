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
            String s = builder.substring(0, start) + builder.substring(end + 1, builder.length());
            builder = new StringBuilder(s);
        }

        return builder.toString().trim();
    }

    public static List<String> getSplitQueries(String query) {
        List<String> list = new ArrayList<>();
        String str = query.trim();
        list.add(str);

        char[] chars = str.toCharArray();
        int prev = 0;

        for (int i = 0; i < chars.length; i++) {
            if (chars[i] != ' ' && i != chars.length - 1) continue;
            if (prev != 0) {
                list.add((str.substring(0, prev) + " " + str.substring(i + 1)).trim());
            }
            prev = i;
        }

        return list;
    }

    public static double jaccard(String str1, String str2) {
//        String[] arr1 = str1.toLowerCase().split(" ");
//        String[] arr2 = str2.toLowerCase().split(" ");
//        Set<String> s1 = new HashSet<>(Arrays.asList(arr1));
//        Set<String> s2 = new HashSet<>(Arrays.asList(arr2));
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

    public static String[] separateDualTitle(String fullTitle) {
        if (fullTitle.indexOf('=') != -1) {
            return Arrays.stream(fullTitle.split("="))
                    .map(String::trim)
                    .collect(Collectors.toList())
                    .toArray(new String[2]);
        } else {
            return Collections.singletonList(fullTitle).toArray(new String[1]);
        }
    }

    public static boolean isExist(List<WebElement> elements) {
        return !elements.isEmpty();
    }
}
