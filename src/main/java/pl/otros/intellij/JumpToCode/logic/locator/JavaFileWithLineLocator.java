package pl.otros.intellij.JumpToCode.logic.locator;

import com.google.common.base.Optional;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import pl.otros.intellij.JumpToCode.IOUtils;
import pl.otros.intellij.JumpToCode.logic.SourceFile;
import pl.otros.intellij.JumpToCode.logic.SourceFileFinder;
import pl.otros.intellij.JumpToCode.model.SourceLocation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JavaFileWithLineLocator implements Locator {

  private static SourceFileFinder sourceFileFinder = new SourceFileFinder();

  @Override
  public List<SourceLocation> findLocation(LocationInfo locationInfo) {
    final Optional<String> pkg = locationInfo.pkg;
    final Optional<String> file = locationInfo.file;
    final Optional<String> line = locationInfo.line;
    if (pkg.isPresent() && file.isPresent() && line.isPresent()) {
      return Collections.singletonList(new SourceLocation(pkg.or(""), file.get(), Integer.parseInt(line.get())));
    }
    return Collections.emptyList();
  }

  @Override
  public List<String> getContent(LocationInfo locationInfo) {
    final List<SourceLocation> location = findLocation(locationInfo);
    final ArrayList<String> result = new ArrayList<String>();
    for (SourceLocation sourceLocation : location) {
      result.addAll(getContentByLine(sourceLocation));
    }
    return result;
  }


  public List<String> getContentByLine(SourceLocation location) {
    List<SourceFile> files = sourceFileFinder.findSourceFiles(location);
    final int lineNumber = location.getLineNumber() - 1;
    List<String> results = new ArrayList<String>();
    for (SourceFile sourceFile : files) {
      final OpenFileDescriptor ofd = new OpenFileDescriptor(sourceFile.getProject(), sourceFile.getVirtualFile(), lineNumber, 1);
      try {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\nPath: ").append(ofd.getFile().getCanonicalPath()).append("\n");
        readFileSelectedLines(lineNumber, ofd.getFile().getInputStream(), stringBuilder);
        results.add(stringBuilder.toString().trim());
      } catch (IOException e) {
        PluginManager.getLogger().error("Can't read source file", e);
      }
    }
    return results;
  }

  void readFileSelectedLines(int lineNumber, InputStream inputStream, StringBuilder stringBuilder) {
    int currentLine = 1;
    BufferedReader bin = null;
    try {
      bin = new BufferedReader(new InputStreamReader(inputStream));
      String s;
      while ((s = bin.readLine()) != null) {
        if (currentLine > lineNumber - 3) {
          stringBuilder.append(currentLine).append(": ").append(s).append("\n");
        }
        if (currentLine > lineNumber + 1) {
          break;
        }
        currentLine++;
      }
    } catch (IOException e) {
      PluginManager.getLogger().error("Can't read source file", e);
    } finally {
      IOUtils.closeQuietly(bin);
    }
  }
}
