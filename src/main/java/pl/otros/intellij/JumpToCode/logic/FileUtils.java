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

package pl.otros.intellij.jumptocode.logic;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.google.common.io.ByteStreams;
import com.intellij.ide.highlighter.JavaClassFileType;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.RunResult;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import pl.otros.intellij.jumptocode.Properties;
import pl.otros.intellij.jumptocode.logic.locator.LocationInfo;
import pl.otros.intellij.jumptocode.logic.locator.Locator;
import pl.otros.intellij.jumptocode.model.JumpLocation;
import pl.otros.intellij.jumptocode.model.PsiModelLocation;
import pl.otros.intellij.jumptocode.model.SourceLocation;
import pl.otros.intellij.jumptocode.server.LoggerConfigResponse;

/**
 *
 */
public class FileUtils {

    public static final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    private SourceFileFinder sourceFileFinder = new SourceFileFinder();

    private final List<? extends Locator> locators;

    public FileUtils(List<? extends Locator> locators) {
        this.locators = locators;
    }

    public List<JumpLocation> findLocation(LocationInfo locationInfo) {

        final ArrayList<JumpLocation> jumpLocations = new ArrayList<JumpLocation>();
        for (Locator l : locators) {
            jumpLocations.addAll(l.findLocation(locationInfo));
        }
        return jumpLocations;
    }


    public List<String> getContent(LocationInfo locationInfo) {
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
    public boolean jumpToLocation(SourceLocation location) {
        List<SourceFile> files = sourceFileFinder.findSourceFiles(location);
        boolean result = false;
        final int lineNumber = location.getLineNumber() - 1;
        for (SourceFile sourceFile : files) {
            final FileEditorManager fem = FileEditorManager.getInstance(sourceFile.getProject());
            final OpenFileDescriptor ofd = new OpenFileDescriptor(sourceFile.getProject(),
                    sourceFile.getVirtualFile(),
                    lineNumber,
                    1);
            ToLineCodeJumper codeJumper = new ToLineCodeJumper(fem, ofd, lineNumber);
            RunResult<Boolean> execute = codeJumper.execute();
            if (execute.getResultObject()) {
                Properties.increaseJumpsCount();
                result = true;
                break;
            }
        }
        return result;
    }

    public boolean jumpToLocation(PsiFile psiFile, int offset, int length) {
        final FileEditorManager fem = FileEditorManager.getInstance(psiFile.getProject());
        final OpenFileDescriptor openFileDescriptor = new OpenFileDescriptor(psiFile.getProject(),
                psiFile.getVirtualFile(),
                offset);
        final ToRangeCodeJumper codeJumper = new ToRangeCodeJumper(fem, openFileDescriptor, offset, length);

        if (codeJumper.execute().getResultObject()) {
            Properties.increaseJumpsCount();
            return true;
        } else {
            return false;
        }

    }

    public List<LoggerConfigResponse> getLoggersConfig() {
        String pattern = "log.*(xml|properties|yaml|yml|json|js)";
        final ArrayList<LoggerConfigResponse> result = new ArrayList<>();
        final Project[] openProjects = ProjectManager.getInstance().getOpenProjects();

        for (Project project : openProjects) {
            final PsiShortNamesCache cache = PsiShortNamesCache.getInstance(project);
            final List<LoggerConfigResponse> inProject = ApplicationManager.getApplication()
                    .runReadAction((Computable<List<LoggerConfigResponse>>) () -> {
                        final String[] allFileNames = cache.getAllFileNames();
                        final ArrayList<LoggerConfigResponse> list = new ArrayList<>();
                        for (String fileName : allFileNames) {
                            if (fileName.matches(pattern)) {
                                final PsiFile[] filesByName = cache.getFilesByName(fileName);
                                for (PsiFile psiFile : filesByName) {
                                    String name = psiFile.getName();
                                    final String fileTypeName = psiFile.getFileType().getName();
                                    String content = psiFile.getViewProvider().getContents().toString();
                                    final LoggerConfigUtil.LoggerType loggerType = LoggerConfigUtil.loggerType(fileName);
                                    if (loggerType == LoggerConfigUtil.LoggerType.Log4j) {
                                        list.add(new LoggerConfigResponse(name,
                                                loggerType.name(),
                                                LoggerConfigUtil.extractLog4jLayoutPatterns(content, fileTypeName)));
                                    } else if (loggerType == LoggerConfigUtil.LoggerType.Log4j2) {
                                        list.add(new LoggerConfigResponse(name,
                                                loggerType.name(),
                                                LoggerConfigUtil.extractLog4j2LayoutPatterns(content,
                                                        psiFile.getFileType().getName())));
                                    } else if (loggerType == LoggerConfigUtil.LoggerType.Logback) {
                                        list.add(new LoggerConfigResponse(name,
                                                loggerType.name(),
                                                LoggerConfigUtil.extractLogbackLayoutPatterns(content)));
                                    }
                                }
                            }
                        }
                        return list;
                    });
            result.addAll(inProject);
        }

        return result;
    }

    public String findWholeClass(String clazz) {
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
                        final PsiFile parentContainingFile = p.getParent().getContainingFile();
                        final FileType parentContainingFileFileType = parentContainingFile.getFileType();

                        return String.format("Binary file. parent is %s, type is %s :%s ",
                                parentContainingFile,
                                parentContainingFileFileType,
                                parentContainingFileFileType.getDefaultExtension());
                    }

                }
            }

        }
        return "";
    }


    private String readVirtualFile(VirtualFile virtualFile) {
        try {
            return new String(ByteStreams.toByteArray(virtualFile.getInputStream()), Charset.forName("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public boolean jumpToLocation(List<JumpLocation> locations) {
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


