package pl.otros.intellij.jumptocode.server;

import java.util.Set;

public class LoggerConfigResponse {
  private String fileName;
  private String type;
  private Set<String> patterns;

  public LoggerConfigResponse() {
  }

  public LoggerConfigResponse(String fileName, String type, Set<String> patterns) {
    this.fileName = fileName;
    this.type = type;
    this.patterns = patterns;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Set<String> getPatterns() {
    return patterns;
  }

  public void setPatterns(Set<String> patterns) {
    this.patterns = patterns;
  }
}
