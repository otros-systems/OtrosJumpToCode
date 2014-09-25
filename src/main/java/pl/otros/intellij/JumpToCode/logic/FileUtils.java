/*
 * Copyright 2014 otros.systems@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pl.otros.intellij.JumpToCode.logic;

import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.PackageIndex;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.log4j.Logger;
import pl.otros.intellij.JumpToCode.IOUtils;
import pl.otros.intellij.JumpToCode.model.SourceLocation;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 */
public class FileUtils {
  private final static Logger logger = Logger.getLogger(FileUtils.class);


  private static final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

  /**
   * find all matching locations in currently opened projects
   *
   * @param location source location to search for
   * @return all matching locations (can be empty)
   */
  public static boolean isReachable(SourceLocation location) {
    return !findSourceFiles(location).isEmpty();
  }


  public static String getContent(SourceLocation location) {
    List<SourceFile> files = findSourceFiles(location);
    final int lineNumber = location.getLineNumber() - 1;
    StringBuilder stringBuilder = new StringBuilder();
    for (SourceFile sourceFile : files) {
      final OpenFileDescriptor ofd = new OpenFileDescriptor(sourceFile.project, sourceFile.virtualFile, lineNumber, 1);
      try {
        stringBuilder.append("\nPath: ").append(ofd.getFile().getCanonicalPath()).append("\n");
        readFileSelectedLines(lineNumber, ofd.getFile().getInputStream(), stringBuilder);
        stringBuilder.append("\n");
      } catch (IOException e) {
        PluginManager.getLogger().error("Can't read source file", e);
      }
    }
    return stringBuilder.toString().trim();
  }

  static void readFileSelectedLines(int lineNumber, InputStream inputStream, StringBuilder stringBuilder) {
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

  /**
   * jump to first matching location
   *
   * @param location the source location to search for
   * @return true if jump was successful
   */
  public static boolean jumpToLocation(SourceLocation location) {
    List<SourceFile> files = findSourceFiles(location);
    boolean result = false;
    final int lineNumber = location.getLineNumber() - 1;
    for (SourceFile sourceFile : files) {
      final FileEditorManager fem = FileEditorManager.getInstance(sourceFile.project);
      final OpenFileDescriptor ofd = new OpenFileDescriptor(sourceFile.project, sourceFile.virtualFile, lineNumber, 1);
      CodeJumper codeJumper = new CodeJumper(fem, ofd, lineNumber);
      invokeSwing(codeJumper, true);
      if (codeJumper.ok) {
        result = true;
        break;
      }
    }
    return result;
  }

  private static void invokeSwing(Runnable runnable, boolean wait) {
    try {
      if (wait) {
        SwingUtilities.invokeAndWait(runnable);
      } else {
        SwingUtilities.invokeLater(runnable);
      }
    } catch (InterruptedException e) {
      logger.error("Interrupted", e);
    } catch (InvocationTargetException e) {
      logger.error("InvocationTargetException", e);
    }
  }


  private static List<SourceFile> findSourceFiles(SourceLocation location) {
    ProjectManager projectManager = ProjectManager.getInstance();
    Project[] projects = projectManager.getOpenProjects();
    List<SourceFile> matches = new ArrayList<SourceFile>();
    for (Project project : projects) {
      ProjectRootManager prm = ProjectRootManager.getInstance(project);
      PackageIndex packageIndex = PackageIndex.getInstance(project);
      ProjectFileIndex fileIndex = prm.getFileIndex();
      VirtualFile[] dirs = packageIndex.getDirectoriesByPackageName(location.getPackageName(), true);
      for (VirtualFile vf : dirs) {
        VirtualFile child = vf.findChild(location.getFileName());
        if (child != null) {
          SourceFile file = new SourceFile(project, fileIndex.getModuleForFile(child), child);
          matches.add(file);
        }
      }
    }
    return matches;
  }

  private static class CodeJumper implements Runnable {
    private boolean ok = false;
    private FileEditorManager fileEditorManager;
    private OpenFileDescriptor ofd;
    private int lineNumber;


    private CodeJumper(FileEditorManager fileEditorManager, OpenFileDescriptor ofd, int lineNumber) {
      this.fileEditorManager = fileEditorManager;
      this.ofd = ofd;
      this.lineNumber = lineNumber;
    }

    public void run() {
      Editor editor = fileEditorManager.openTextEditor(ofd, true);
      if (editor != null && lineNumber>=0) {
        final TextAttributesKey searchResultAttributes = EditorColors.SEARCH_RESULT_ATTRIBUTES;
        final TextAttributes attributes = searchResultAttributes.getDefaultAttributes();
        RangeHighlighter highlighter = editor.getMarkupModel().addLineHighlighter(lineNumber, HighlighterLayer.ERROR, attributes);
        scheduledExecutorService.schedule(new RemoveHighLighter(editor, highlighter), 5, TimeUnit.SECONDS);
        ok = true;
      }
    }
  }

  private static class RemoveHighLighter implements Runnable {
    private Editor editor;
    private RangeHighlighter highlighter;

    private RemoveHighLighter(Editor editor, RangeHighlighter highlighter) {
      this.editor = editor;
      this.highlighter = highlighter;
    }

    public void run() {
      invokeSwing(new Runnable() {
        public void run() {
          editor.getMarkupModel().removeHighlighter(highlighter);
        }
      }, false);
    }
  }

  private static class SourceFile {
    Module module;
    VirtualFile virtualFile;
    private Project project;

    private SourceFile(Project project, Module module, VirtualFile virtualFile) {
      this.project = project;
      this.module = module;
      this.virtualFile = virtualFile;
    }

    @Override
    public String toString() {
      String moduleName = (module != null) ? module.getName() : "null";
      String projectName = (project != null) ? project.getName() : "null";
      return String.format("project=[%s] module=[%s] path=[%s}",
          projectName, moduleName, virtualFile.getPath());
    }
  }
}
