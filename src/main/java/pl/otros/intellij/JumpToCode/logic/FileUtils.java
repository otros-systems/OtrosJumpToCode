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

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.intellij.ide.highlighter.JavaClassFileType;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.PackageIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.JavaPsiFacadeEx;
import com.intellij.psi.impl.source.PsiClassImpl;
import com.intellij.psi.impl.source.PsiMethodImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import pl.otros.intellij.JumpToCode.IOUtils;
import pl.otros.intellij.JumpToCode.Properties;
import pl.otros.intellij.JumpToCode.model.JumpLocation;
import pl.otros.intellij.JumpToCode.model.PsiModelLocation;
import pl.otros.intellij.JumpToCode.model.SourceLocation;

import javax.swing.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 */
public class FileUtils {
  private final static Logger logger = Logger.getLogger(FileUtils.class);


  static final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
  private static final int READ_LIMIT = 100 * 1000;

  /**
   * find all matching locations in currently opened projects
   *
   * @return all matching locations (can be empty)
   */
  public static boolean isReachable(Optional<String> pkg,
                                    Optional<String> clazz,
                                    Optional<String> file,
                                    Optional<String> line,
                                    Optional<String> message) {
    return !findLocation(pkg, clazz, file, line, message).isEmpty();
  }


  public static List<JumpLocation> findLocation(Optional<String> pkg,
                                                Optional<String> clazz,
                                                Optional<String> file,
                                                Optional<String> line,
                                                Optional<String> message) {
    final ArrayList<JumpLocation> jumpLocations = new ArrayList<JumpLocation>();
    if (clazz.isPresent() && message.isPresent()) {
      String fqcn = StringUtils.isEmpty(pkg.or("")) ? clazz.get() : pkg.get() + "." + clazz.get();
      String msg = message.get();
      jumpLocations.addAll(findByLogMessage(fqcn, msg));
    }
    if (file.isPresent() && line.isPresent() && StringUtils.isNotBlank(line.get()) && StringUtils.isNumeric(line.get())) {
      jumpLocations.add(new SourceLocation(pkg.or(""), file.get(), Integer.parseInt(line.get())));
    }

    return jumpLocations;
  }

  private static List<? extends JumpLocation> findByLogMessage(final String fqcn, final String code) {
    final ArrayList<JumpLocation> jumpLocations = new ArrayList<JumpLocation>();

    ProjectManager projectManager = ProjectManager.getInstance();
    Project[] projects = projectManager.getOpenProjects();

    for (final Project project : projects) {

      final List<? extends JumpLocation> result = ApplicationManager.getApplication().runReadAction(new Computable<List<? extends JumpLocation>>() {
        public List<? extends JumpLocation> compute() {
          final PsiClass aClass = JavaPsiFacadeEx.getInstanceEx(project).findClass(fqcn);
          final ArrayList<JumpLocation> result = new ArrayList<JumpLocation>();
          final JavaRecursiveElementVisitor javaRecursiveElementVisitor = new JavaRecursiveElementVisitor() {
            @Override
            public void visitReferenceElement(PsiJavaCodeReferenceElement reference) {
              super.visitReferenceElement(reference);

              final PsiElement context = reference.getContext();

              //find all method invocation
              if (context instanceof PsiMethodCallExpression) {
                PsiMethodCallExpression mc = (PsiMethodCallExpression) context;
                if (((PsiReferenceExpression) mc.getFirstChild()).resolve() != null) {
                  final PsiMethod psiMethod = (PsiMethod) ((PsiReferenceExpression) mc.getFirstChild()).resolve();
                  if (psiMethod != null) {
                    final PsiClass containingClass = psiMethod.getContainingClass();
                    if (containingClass != null) {
                      String caller = Optional.fromNullable(containingClass.getQualifiedName()).or("");
                      if (caller.contains("Logger") || caller.contains("log4j.Category")) {
                        final List<PsiLiteralExpression> psiLiteralExpressions = literalExpression(mc.getArgumentList().getExpressions());
                        for (PsiLiteralExpression psiLiteralExpression : psiLiteralExpressions) {
                          String text = unwrap(psiLiteralExpression.getText());
                          if (code.contains(text)) {
                            final int textOffset = mc.getTextOffset();
                            final int textLength = mc.getTextLength();
                            final PsiFile containingFile = aClass.getContainingFile();
                            result.add(new PsiModelLocation(containingFile, textOffset, textLength, mc, extractParent(mc)));
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          };
          if (aClass != null) {
            javaRecursiveElementVisitor.visitElement(aClass);
          }
          return result;
        }
      });
      jumpLocations.addAll(result);
    }
    return jumpLocations;
  }

  public static Optional<PsiElement> extractParent(PsiElement psiElement) {
    final PsiElement parent = psiElement.getParent();
    if (parent != null) {
      if (parent instanceof PsiMethodImpl || parent instanceof PsiClassImpl) {
        return Optional.of(parent);
      } else {
        return extractParent(parent);
      }
    } else {
      return Optional.absent();
    }
  }


  public static List<String> getContent(Optional<String> pkg,
                                        Optional<String> clazz,
                                        Optional<String> file,
                                        Optional<String> line,
                                        Optional<String> message) {
    final ArrayList<String> r = new ArrayList<String>();
    if (clazz.isPresent() && message.isPresent()) {
      String fqcn = StringUtils.isEmpty(pkg.or("")) ? clazz.get() : pkg.get() + "." + clazz.get();
      String msg = message.get();
      r.addAll(getContentByMessage(fqcn, msg));
    }
    if (file.isPresent() && line.isPresent() && StringUtils.isNotBlank(line.get()) && StringUtils.isNumeric(line.get())) {
      final SourceLocation location = new SourceLocation(pkg.or(""), file.get(), Integer.parseInt(line.get()));
      r.addAll(getContentByLine(location));
    }

    return r;
  }

  public static List<String> getContentByMessage(final String fqcn, final String code) {
    final ArrayList<String> list = new ArrayList<String>();
    final List<? extends JumpLocation> locations = findByLogMessage(fqcn, code);
    for (JumpLocation location : locations) {
      if (location instanceof PsiModelLocation) {
        final PsiModelLocation psiModelLocation = (PsiModelLocation) location;
        final VirtualFile virtualFile = psiModelLocation.getContainingFile().getContainingFile().getVirtualFile();
        try {
          final String content = new String(virtualFile.contentsToByteArray());
          final Function<PsiElement, Integer> psiElementToOffset = new PsiElementIntegerFunction();
          final int methodCallStart = psiModelLocation.getTextOffset();
          int start = 0;
          Optional<Integer> methodCallOffset = psiModelLocation.getParent().transform(psiElementToOffset);
          if (methodCallOffset.isPresent()) {
            start = StringUtils.lastIndexOf(content.substring(0, methodCallOffset.get()), '\n');
          } else {
            final String allFile = readVirtualFile(((PsiModelLocation) location).getContainingFile().getVirtualFile());
            final ArrayList<Integer> newLinesPositions = new ArrayList<Integer>();
            for (int i = 0; i < methodCallStart && i < allFile.length(); i++) {
              if (allFile.charAt(i) == '\n') {
                newLinesPositions.add(i);
              }
            }
            start = newLinesPositions.get(Math.max(newLinesPositions.size() - 5, 0));
          }
          int end = StringUtils.indexOf(content, '\n', methodCallStart + psiModelLocation.getTextLength());
          String toDisplay = content.substring(start, end);
          final String[] lines = toDisplay.split("\n");
          final StringBuilder sb = new StringBuilder();
          sb.append("\nPath: ").append(((PsiModelLocation) location).getContainingFile().getVirtualFile().getCanonicalPath()).append("\n");
          if (lines.length > 10) {
            for (int i = 0; i < 4; i++) {
              sb.append(lines[i]).append("\n");
            }
            sb.append(".......\n.......\n");
            for (int i = lines.length - 5; i < lines.length - 1; i++) {
              sb.append(lines[i]).append("\n");
            }
          } else {
            sb.append(toDisplay);
          }
          list.add(sb.toString());
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    return list;
  }


  public static List<String> getContentByLine(SourceLocation location) {
    List<SourceFile> files = findSourceFiles(location);
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

  public static boolean jumpToLocation(PsiFile psiFile, int offset, int length) {
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
      VirtualFile[] dirs = packageIndex.getDirectoriesByPackageName(location.getPackageName(), true);
      for (VirtualFile vf : dirs) {
        VirtualFile child = vf.findChild(location.getFileName());
        if (child != null) {
          SourceFile file = new SourceFile(project, child);
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
          final FileType fileType = containingFile.getFileType();
          final String defaultExtension = fileType.getDefaultExtension();

          if (fileType instanceof JavaFileType) {
            final VirtualFile virtualFile = containingFile.getVirtualFile();
            return readVirtualFile(virtualFile);
          } else if (fileType instanceof JavaClassFileType) {
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

  private static String unwrap(String text) {
    if (text.startsWith("\"") && text.endsWith("\"") && text.length() > 2) {
      return text.substring(1, text.length() - 1);
    } else {
      return text;
    }
  }

  private static List<PsiLiteralExpression> literalExpression(PsiExpression[] expressions) {
    List<PsiLiteralExpression> r = new ArrayList<PsiLiteralExpression>();
    for (PsiExpression e : expressions) {
      if (e instanceof PsiLiteralExpression) {
        r.add((PsiLiteralExpression) e);
      } else if (e instanceof PsiBinaryExpression) {
        PsiBinaryExpression be = (PsiBinaryExpression) e;
        r.addAll(literalExpression(be.getOperands()));
      }
    }
    return r;
  }

  public static boolean jumpToLocation(List<JumpLocation> locations) {
    for (JumpLocation location : locations) {
      if (location instanceof PsiModelLocation) {
        PsiModelLocation l = (PsiModelLocation) location;
        return jumpToLocation(l.getContainingFile(), l.getTextOffset(), l.getTextLength());
      } else if (location instanceof SourceLocation) {
        SourceLocation sl = (SourceLocation) location;
        return jumpToLocation(sl);
      }
    }
    return false;
  }

  private static class PsiElementIntegerFunction implements Function<PsiElement, Integer> {
    @Override
    public Integer apply(PsiElement psiElement) {
      return psiElement.getTextOffset();
    }
  }
}


