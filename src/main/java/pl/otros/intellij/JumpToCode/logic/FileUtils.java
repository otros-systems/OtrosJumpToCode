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

import com.google.common.io.ByteStreams;
import com.intellij.ide.highlighter.JavaClassFileType;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import pl.otros.intellij.JumpToCode.Properties;
import pl.otros.intellij.JumpToCode.gui.SwingUtils;
import pl.otros.intellij.JumpToCode.logic.locator.JavaFileWithLineLocator;
import pl.otros.intellij.JumpToCode.logic.locator.JavaPisLocator;
import pl.otros.intellij.JumpToCode.logic.locator.LocationInfo;
import pl.otros.intellij.JumpToCode.logic.locator.Locator;
import pl.otros.intellij.JumpToCode.model.JumpLocation;
import pl.otros.intellij.JumpToCode.model.PsiModelLocation;
import pl.otros.intellij.JumpToCode.model.SourceLocation;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 */
public class FileUtils {

  public static final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

  private static SourceFileFinder sourceFileFinder = new SourceFileFinder();

  private static List<? extends Locator> locators = Arrays.asList(new JavaPisLocator(), new JavaFileWithLineLocator());


  public static List<JumpLocation> findLocation(LocationInfo locationInfo) {
    final ArrayList<JumpLocation> jumpLocations = new ArrayList<JumpLocation>();
    for (Locator l : locators) {
      jumpLocations.addAll(l.findLocation(locationInfo));
    }
    return jumpLocations;
  }


  public static List<String> getContent(LocationInfo locationInfo) {
    final ArrayList<String> r = new ArrayList<String>();
    for (Locator l : locators) {
      r.addAll(l.getContent(locationInfo));
    }
    return r;
  }


  /**
   * jump to first matching location
   *
   * @param location the source location to search for
   * @return true if jump was successful
   */
  public static boolean jumpToLocation(SourceLocation location) {
    List<SourceFile> files = sourceFileFinder.findSourceFiles(location);
    boolean result = false;
    final int lineNumber = location.getLineNumber() - 1;
    for (SourceFile sourceFile : files) {
      final FileEditorManager fem = FileEditorManager.getInstance(sourceFile.getProject());
      final OpenFileDescriptor ofd = new OpenFileDescriptor(sourceFile.getProject(), sourceFile.getVirtualFile(), lineNumber, 1);
      ToLineCodeJumper codeJumper = new ToLineCodeJumper(fem, ofd, lineNumber);
      SwingUtils.invokeSwing(codeJumper, true);
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
    SwingUtils.invokeSwing(codeJumper, true);
    boolean result = false;
    if (codeJumper.isOk()) {
      Properties.increaseJumpsCount();
      result = true;
    }
    return result;
  }


  public static String findWholeClass(String clazz) {
    final PsiShortNamesCache instance = PsiShortNamesCache.getInstance(ProjectManager.getInstance().getDefaultProject());
    ProjectManager projectManager = ProjectManager.getInstance();
    Project[] projects = projectManager.getOpenProjects();
    for (Project project : projects) {
      final JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
      final PsiClass[] classes = psiFacade.findClasses(clazz, GlobalSearchScope.allScope(project));
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
          } else if ("scala".equals(defaultExtension)) {
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
      final PsiClass[] classesByName = instance.getClassesByName(clazz, GlobalSearchScope.projectScope(project));
      final String[] allClassNames = instance.getAllClassNames();

    }
    return "";
  }


  private static String readVirtualFile(VirtualFile virtualFile) {
    try {
      return new String(ByteStreams.toByteArray(virtualFile.getInputStream()), Charset.forName("UTF-8"));
    } catch (IOException e) {
      e.printStackTrace();
    }
    return "";
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


}


