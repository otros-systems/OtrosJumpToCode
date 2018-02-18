package pl.otros.intellij.jumptocode.logic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.json.JsonFileType;
import com.intellij.openapi.diagnostic.Logger;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LoggerConfigUtil {

  private static final Logger LOGGER = PluginManager.getLogger();

  public static LoggerType loggerType(String fileName) {
    if (fileName.startsWith("log4j2")) {
      return LoggerType.Log4j2;
    } else if (fileName.startsWith("log4j")) {
      return LoggerType.Log4j;
    } else if (fileName.startsWith("logback")) {
      return LoggerType.Logback;
    } else {
      return LoggerType.Unknown;
    }
  }

  public static Set<String> extractLog4jLayoutPatterns(String content, String fileTypeName) {
    Set<String> result = new HashSet<>();
    if ("XML".equalsIgnoreCase(fileTypeName)) {
      final Matcher log4jMatcher = Pattern.compile("<param\\s*name=\"ConversionPattern\"\\s*value=\"(.*?)\".*", Pattern.MULTILINE).matcher(content);
      while (log4jMatcher.find()) {
        result.add(log4jMatcher.group(1));
      }
    } else if ("Properties".equalsIgnoreCase(fileTypeName)) {
      try {
        final Properties properties = new Properties();
        properties.load(new StringReader(content));
        List<String> patterns = properties
            .<String>keySet()
            .stream()
            .map(Object::toString)
            .filter(key -> key.endsWith("ConversionPattern"))
            .map(properties::getProperty)
            .collect(Collectors.toList());
        result.addAll(patterns);
      } catch (IOException e1) {
        LOGGER.warn("Can't read content as properties: " + e1.getMessage());
      }
    }

    return result;
  }

  public static Set<String> extractLog4j2LayoutPatterns(String content, String fileType) {
    Set<String> result = new HashSet<>();
    if (XmlFileType.INSTANCE.getName().equals(fileType)) {
      final Matcher log4j2Matcher = Pattern.compile("<pattern>\\s*(.*?)\\s*</pattern>", Pattern.MULTILINE).matcher(content);
      while (log4j2Matcher.find()) {
        result.add(log4j2Matcher.group(1));
      }
      final Matcher log4jMatcher2 = Pattern.compile("<PatternLayout.*?pattern=\"(.*?)\".*", Pattern.MULTILINE).matcher(content);
      while (log4jMatcher2.find()) {
        result.add(log4jMatcher2.group(1));
      }
    } else if ("YAML".equalsIgnoreCase(fileType)) {
      Arrays.stream(content.split("\n"))
          .map(String::trim)
          .filter(line -> line.startsWith("Pattern:"))
          .map(line -> line.replaceFirst("Pattern:", "").trim())
          .map(line -> line.replaceFirst("^\"", "").replaceFirst("\"$", ""))
          .forEach(result::add);

    } else if ("Properties".equalsIgnoreCase(fileType)) {
      try {
        final Properties properties = new Properties();
        properties.load(new StringReader(content));
        properties
            .<String>keySet()
            .stream()
            .map(Object::toString)
            .filter(key -> key.endsWith("layout.pattern"))
            .map(properties::getProperty)
            .forEach(result::add);
      } catch (IOException e1) {
        LOGGER.warn("Can't read content as properties: " + e1.getMessage());
      }
    } else if (JsonFileType.INSTANCE.getName().equals(fileType)) {
      final JsonElement json = new JsonParser().parse(content);
      System.out.println("LoggerConfigUtil.extractLog4j2LayoutPatterns " + json);
      result.addAll(searchForPatterns(json, ""));
    }
    return result;
  }

  private static List<String> searchForPatterns(JsonElement jsonElement, String parent) {
    if (jsonElement.isJsonObject()) {
      final JsonObject object = jsonElement.getAsJsonObject();
      return object
          .keySet()
          .stream()
          .flatMap(key -> {
            final JsonElement jsonElement1 = object.get(key);
            if (jsonElement1.isJsonObject() || jsonElement1.isJsonArray()) {
              return searchForPatterns(jsonElement1, key).stream();
            } else if (key.equals("pattern") && parent.equals("PatternLayout")) {
              return Stream.of(object.getAsJsonPrimitive(key).getAsString());
            } else {
              return Stream.empty();
            }
          })
          .collect(Collectors.toList());

    } else if (jsonElement.isJsonArray()) {
      JsonArray array = jsonElement.getAsJsonArray();
      final ArrayList<String> strings = new ArrayList<>();
      for (int i = 0; i < array.size(); i++) {
        final JsonElement elementI = array.get(i);
        if (elementI.isJsonArray() || elementI.isJsonObject()) {
          strings.addAll(searchForPatterns(elementI, parent));
        }
      }
      return strings;
    } else {
      return Collections.emptyList();
    }
  }

  public static Set<String> extractLogbackLayoutPatterns(String content) {

    Set<String> result = new HashSet<>();
    final Matcher logbackMatcher = Pattern.compile("<pattern>\\s*(.*?)\\s*</pattern>", Pattern.MULTILINE + Pattern.CASE_INSENSITIVE).matcher(content);
    while (logbackMatcher.find()) {
      result.add(logbackMatcher.group(1));
    }

    return result;
  }

  enum LoggerType {
    Log4j, Log4j2, Logback, Unknown
  }

}
