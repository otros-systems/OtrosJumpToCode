package pl.otros.intellij.jumptocode.logic;

import com.google.common.io.CharStreams;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static pl.otros.intellij.jumptocode.logic.LoggerConfigUtil.*;

public class LoggerConfigUtilTest {

  @Test
  public void loggerType() {
    assertEquals(LoggerConfigUtil.LoggerType.Log4j, LoggerConfigUtil.loggerType("log4j-test.xml"));
    assertEquals(LoggerConfigUtil.LoggerType.Log4j, LoggerConfigUtil.loggerType("log4j.xml"));
    assertEquals(LoggerConfigUtil.LoggerType.Log4j2, LoggerConfigUtil.loggerType("log4j2-test.xml"));
    assertEquals(LoggerConfigUtil.LoggerType.Log4j2, LoggerConfigUtil.loggerType("log4j2.xml"));
    assertEquals(LoggerConfigUtil.LoggerType.Log4j2, LoggerConfigUtil.loggerType("log4j2.json"));
    assertEquals(LoggerConfigUtil.LoggerType.Logback, LoggerConfigUtil.loggerType("logback-test.xml"));
    assertEquals(LoggerConfigUtil.LoggerType.Logback, LoggerConfigUtil.loggerType("logback.xml"));
    assertEquals(LoggerConfigUtil.LoggerType.Unknown, LoggerConfigUtil.loggerType("logging.xml"));
  }

  @Test
  public void extractLog4jLayoutPatternsFromXml() throws IOException {
    assertThat(extractLog4jLayoutPatterns(content("log4j.xml"), "XML")).containsOnly(
        "%d %-5p: %c - %m%n",
        "%d %-5p: %c [%t] %m%n");
  }

  @Test
  public void extractLog4jLayoutPatternsFromProperties() throws IOException {
    assertThat(extractLog4jLayoutPatterns(content("log4j.properties"), "Properties")).containsOnly("%d %t %p [%c] - %m%n");
  }


  @Test
  public void extractLog4j2LayoutPatternsFromJson() throws IOException {
    assertThat(extractLog4j2LayoutPatterns(content("log4j2.json"), "JSON")).containsOnly(
        "%d %p %c{1.} [%t] %m%n",
        "%m%n");
  }

  @Test
  public void extractLog4j2LayoutPatternsFromXml() throws IOException {
    assertThat(extractLog4j2LayoutPatterns(content("log4j2.xml"), "XML")).containsOnly(
        "%d %p %c{1.} [%t] %m%n",
        "%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n");
  }

  @Test
  public void extractLog4j2LayoutPatternsFromProperties() throws IOException {
    assertThat(extractLog4j2LayoutPatterns(content("log4j2.properties"), "Properties")).containsOnly(
        "%d %m%n",
        "%d %p %C{1.} [%t] %m%n");
  }

  @Test
  public void extractLog4j2LayoutPatternsFromYaml() throws IOException {
    assertThat(extractLog4j2LayoutPatterns(content("log4j2.yaml"), "YAML")).containsOnly(
        "%d %p %C{1.} [%t] %m%n",
        "%m%n"
    );
  }


  @Test
  public void extractLogbackLayoutPatternsFromXml() throws IOException {
    final Set<String> actual = extractLogbackLayoutPatterns(content("logback.xml"));
    actual.forEach(System.out::println);
    assertThat(actual).containsOnly("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
  }

  @NotNull
  private String content(String file) throws IOException {
    return CharStreams.toString(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(file)));
  }

}