package pl.otros.intellij.jumptocode.logic.locator;

import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Optional;

public class LocationInfo {


  public final Optional<String> pkg;
  public final Optional<String> clazz;
  public final Optional<String> file;
  public final Optional<String> line;
  public final Optional<String> msg;

  public LocationInfo(Optional<String> pkg,
                      Optional<String> clazz,
                      Optional<String> file,
                      Optional<String> line,
                      Optional<String> msg) {
    this.pkg = pkg;
    this.clazz = clazz;
    this.file = file;
    this.line = line;
    this.msg = msg;
  }

  public static LocationInfo parse(HttpServletRequest request) {
    final Optional<String> pkg = getOptParameter(request, "p", "packageName");
    final Optional<String> clazz = getOptParameter(request, "c", "className");
    final Optional<String> file = getOptParameter(request, "f", "file");
    final Optional<String> line = getOptParameter(request, "l", "lineNumber");
    final Optional<String> msg = getOptParameter(request, "m", "message");
    return new LocationInfo(pkg, clazz, file, line, msg);
  }

  private static Optional<String> getOptParameter(HttpServletRequest request, String shortName, String longName) {
    String value = request.getParameter(longName);
    if (value == null) {
      value = request.getParameter(shortName);
    }
    return Optional.fromNullable(value);
  }

}
