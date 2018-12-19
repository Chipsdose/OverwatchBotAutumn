package de.metahero.discord.overwatchbot.util;

import static java.lang.String.format;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import static java.util.Objects.nonNull;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BattleTagSrReader {
    public static int getSeasonRatingFor(String battleTag) {
        battleTag = battleTag.replace("#", "-");
        try {
            final Document d = Jsoup.connect(String.format("https://playoverwatch.com/de-de/career/pc/%s", battleTag)).timeout(20000).get();
            final Elements element = d.getElementsByAttributeValue("class", "competitive-rank");
            if (nonNull(element)) {
                final String elementSr = element.text();
                final Pattern srPattern = Pattern.compile("\\d{3,4}");
                final Matcher matcher = srPattern.matcher(elementSr);
                if (matcher.find()) {
                    final String sr = matcher.group();
                    try {
                        return Integer.parseInt(sr);
                    } catch (final NumberFormatException e) {
                        log.error("[{}] is not an Integer", elementSr);
                    }
                } else {
                    throw new Exception(format("No SR Number found in [%s]", element.text()));
                }
            }
        } catch (final Exception e) {
            log.error("Failed to load battleTag[{}] - Message: {}", battleTag, e.getMessage(), e);
            return -888;
        }
        return -999;
    }
}
