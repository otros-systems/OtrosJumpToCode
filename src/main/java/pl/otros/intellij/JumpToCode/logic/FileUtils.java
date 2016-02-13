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

import com.intellij.ide.highlighter.JavaClassFileType;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.PackageIndex;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import org.apache.log4j.Logger;
import pl.otros.intellij.JumpToCode.IOUtils;
import pl.otros.intellij.JumpToCode.Properties;
import pl.otros.intellij.JumpToCode.model.SourceLocation;

import javax.swing.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 */
public class FileUtils {
  private final static Logger logger = Logger.getLogger(FileUtils.class);


  static final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
  private static final int READ_LIMIT = 100 * 1000;

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
      final OpenFileDescriptor ofd = new OpenFileDescriptor(sourceFile.getProject(), sourceFile.getVirtualFile(), lineNumber, 1);
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
      final FileEditorManager fem = FileEditorManager.getInstance(sourceFile.getProject());
      final OpenFileDescriptor ofd = new OpenFileDescriptor(sourceFile.getProject(), sourceFile.getVirtualFile(), lineNumber, 1);
      ToLineCodeJumper codeJumper = new ToLineCodeJumper(fem, ofd, lineNumber);
      invokeSwing(codeJumper, true);
      if (codeJumper.isOk()) {
        Properties.increaseJumpsCount();
        result = true;
        break;
      }
    }
    return result;
  }

  public static boolean jumpToLoccation(PsiFile psiFile, int offset, int length) {
    final FileEditorManager fem = FileEditorManager.getInstance(psiFile.getProject());
    final OpenFileDescriptor openFileDescriptor = new OpenFileDescriptor(psiFile.getProject(), psiFile.getVirtualFile(), offset);
    final ToRangeCodeJumper codeJumper = new ToRangeCodeJumper(fem, openFileDescriptor, offset, length);
    invokeSwing(codeJumper, true);
    boolean result = false;
    if (codeJumper.isOk()) {
      Properties.increaseJumpsCount();
      result = true;
    }
    return result;
  }


  static void invokeSwing(Runnable runnable, boolean wait) {
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


  public static String findWholeClass(String clazz) {
    final PsiShortNamesCache instance = PsiShortNamesCache.getInstance(ProjectManager.getInstance().getDefaultProject());
    ProjectManager projectManager = ProjectManager.getInstance();
    Project[] projects = projectManager.getOpenProjects();
    List<PsiFile> matches = new ArrayList<PsiFile>();
    for (Project project : projects) {
      final JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
      final PsiClass[] classes = psiFacade.findClasses(clazz, GlobalSearchScope.allScope(project));
      System.out.println("Found " + classes + " java classes");
      for (PsiClass p : classes) {
        //check if this is source
        if (p.canNavigateToSource()) {
          final PsiFile containingFile = p.getContainingFile();

//          final OpenFileDescriptor openFileDescriptor = new OpenFileDescriptor(project, containingFile.getVirtualFile(), 1, 1);
//          if (openFileDescriptor.canNavigateToSource()){
//            System.out.println("Can navigate to source");
//            final VirtualFile file = openFileDescriptor.getFile();
//            return readVirtualFile(file);
//          }
          final FileType fileType = containingFile.getFileType();
          final String defaultExtension = fileType.getDefaultExtension();

          if (fileType instanceof JavaFileType) {
            JavaFileType file = (JavaFileType) fileType;
            final VirtualFile virtualFile = containingFile.getVirtualFile();
            return readVirtualFile(virtualFile);
          } else if (fileType instanceof JavaClassFileType) {
            JavaClassFileType javaClassFileType = (JavaClassFileType) fileType;
            //TODO get source of class?
            return "";
          } else if (defaultExtension.equals("scala")) {
            final VirtualFile virtualFile = containingFile.getVirtualFile();
            return readVirtualFile(virtualFile);
          } else if (fileType.isBinary()) {
            final PsiFile paretntContainingFile = p.getParent().getContainingFile();
            final FileType paretntContainingFileFileType = paretntContainingFile.getFileType();

            return String.format("Binary file. parent is %s, type is %s :%s ", paretntContainingFile, paretntContainingFileFileType, paretntContainingFileFileType.getDefaultExtension());
          }

        }
      }

      final PsiFile[] filesByName = instance.getFilesByName(clazz);
      System.out.println("Found " + filesByName.length + " files by name");

      final PsiClass[] classesByName = instance.getClassesByName(clazz, GlobalSearchScope.projectScope(project));
      System.out.println("Found " + classesByName.length + " classes by name");

      final String[] allClassNames = instance.getAllClassNames();
      System.out.println("Found " + allClassNames.length + " all by name");

      for (PsiClass psiClass : classesByName) {
        final PsiFile containingFile = psiClass.getContainingFile();
        matches.add(containingFile);
        System.out.println("Is in file: " + containingFile.getName());
      }
    }
    return "";
  }


  private static String readVirtualFile(VirtualFile virtualFile) {
    try {
      final InputStream inputStream = virtualFile.getInputStream();
      byte[] buff = new byte[1024];
      int read = 0;
      ByteArrayOutputStream bin = new ByteArrayOutputStream(inputStream.available());
      while ((read = inputStream.read(buff)) > 0 && read < READ_LIMIT) {
        bin.write(buff, 0, read);
      }
      return new String(bin.toByteArray(), Charset.forName("UTF-8"));
    } catch (IOException e) {
      e.printStackTrace();
    }
    return "";
  }


}

class SourceFile {
  Module module;
  VirtualFile virtualFile;
  private Project project;

  SourceFile(Project project, Module module, VirtualFile virtualFile) {
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

  public Module getModule() {
    return module;
  }

  public VirtualFile getVirtualFile() {
    return virtualFile;
  }

  public Project getProject() {
    return project;
  }
}

class ToLineCodeJumper implements Runnable {
  private boolean ok = false;
  private FileEditorManager fileEditorManager;
  private OpenFileDescriptor ofd;
  private int lineNumber;


  ToLineCodeJumper(FileEditorManager fileEditorManager, OpenFileDescriptor ofd, int lineNumber) {
    this.fileEditorManager = fileEditorManager;
    this.ofd = ofd;
    this.lineNumber = lineNumber;
  }

  public void run() {
    Editor editor = fileEditorManager.openTextEditor(ofd, true);
    if (editor != null && lineNumber >= 0) {
      final TextAttributesKey searchResultAttributes = EditorColors.SEARCH_RESULT_ATTRIBUTES;
      final TextAttributes attributes = searchResultAttributes.getDefaultAttributes();
      RangeHighlighter highlighter = editor.getMarkupModel().addLineHighlighter(lineNumber, HighlighterLayer.ERROR, attributes);
      FileUtils.scheduledExecutorService.schedule(new RemoveHighLighter(editor, highlighter), 5, TimeUnit.SECONDS);
      ok = true;
    }
  }

  public boolean isOk() {
    return ok;
  }
}


class ToRangeCodeJumper implements Runnable {
  private boolean ok = false;
  private FileEditorManager fileEditorManager;
  private OpenFileDescriptor ofd;
  private int offset;
  private int length;


  ToRangeCodeJumper(FileEditorManager fileEditorManager, OpenFileDescriptor ofd, int offset, int length) {
    this.fileEditorManager = fileEditorManager;
    this.ofd = ofd;
    this.offset = offset;
    this.length = length;
  }

  public void run() {
    Editor editor = fileEditorManager.openTextEditor(ofd, true);
    if (editor != null && offset >= 0) {
      final TextAttributesKey searchResultAttributes = EditorColors.SEARCH_RESULT_ATTRIBUTES;
      final TextAttributes attributes = searchResultAttributes.getDefaultAttributes();
      RangeHighlighter highlighter = editor.getMarkupModel().addRangeHighlighter(offset, offset + length, HighlighterLayer.SELECTION, attributes, HighlighterTargetArea.LINES_IN_RANGE);

      FileUtils.scheduledExecutorService.schedule(new RemoveHighLighter(editor, highlighter), 5, TimeUnit.SECONDS);
      ok = true;
    }
  }

  public boolean isOk() {
    return ok;
  }
}

class RemoveHighLighter implements Runnable {
  private Editor editor;
  private RangeHighlighter highlighter;

  public RemoveHighLighter(Editor editor, RangeHighlighter highlighter) {
    this.editor = editor;
    this.highlighter = highlighter;
  }

  public void run() {
    FileUtils.invokeSwing(new Runnable() {
      public void run() {
        editor.getMarkupModel().removeHighlighter(highlighter);
      }
    }, false);
  }
}
