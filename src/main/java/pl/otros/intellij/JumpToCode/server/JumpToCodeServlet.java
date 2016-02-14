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

package pl.otros.intellij.JumpToCode.server;

import com.google.common.base.Optional;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import org.apache.commons.lang.StringUtils;
import pl.otros.intellij.JumpToCode.logic.FileCopyUtils;
import pl.otros.intellij.JumpToCode.logic.FileUtils;
import pl.otros.intellij.JumpToCode.model.JumpLocation;
import pl.otros.intellij.JumpToCode.model.SourceLocation;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;

/**
 */
public class JumpToCodeServlet extends HttpServlet {


  private static String getParameter(HttpServletRequest request, String shortName, String longName) {
    String value = request.getParameter(longName);
    if (value == null) {
      value = request.getParameter(shortName);
    }
    return StringUtils.defaultString(value, "");
  }

  private static Optional<String> getOptParameter(HttpServletRequest request, String shortName, String longName) {
    String value = request.getParameter(longName);
    if (value == null) {
      value = request.getParameter(shortName);
    }
    return Optional.fromNullable(value);
  }

  private static String getParameter(HttpServletRequest request, String shortName, String longName, String defaultValue) {
    String value = getParameter(request, shortName, longName);
    return (value != null) ? value : defaultValue;
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    response.setContentType("text/html");
    response.addHeader("ide", "idea");

    String operation = getParameter(request, "operation", "o", "form");
    if (operation.equals("form")) {
      form(response);
      return;
    }
    if (StringUtils.equalsIgnoreCase("jump", operation)) {
      jump(request, response, false);
    } else if (StringUtils.equalsIgnoreCase("test", operation)) {
      jump(request, response, true);
    } else if (StringUtils.equalsIgnoreCase("content", operation)) {
      content(request, response);
    } else if (StringUtils.equalsIgnoreCase("all", operation)) {
      final String wholeClass = FileUtils.findWholeClass(getParameter(request, "c", "className"));
      response.setContentType("text/plain");
      if (wholeClass.length() > 0) {
        response.getWriter().print(wholeClass);
      } else {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
      }
    } else if (StringUtils.equalsIgnoreCase("new", operation)) {
      try {
        newOperation(request, response);
      } catch (Exception e) {
        e.printStackTrace(response.getWriter());
      }
    } else {
      error(response, "Unexpected operation");
    }
  }

  private void newOperation(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String packageName = getParameter(request, "p", "packageName");
    final String fqcn = getParameter(request, "c", "className");
    final String code = getParameter(request, "d", "code");

    ProjectManager projectManager = ProjectManager.getInstance();
    Project[] projects = projectManager.getOpenProjects();
    final PrintWriter writer = response.getWriter();

    response.setContentType("text/plain");
    writer.println("Response for " + fqcn + " with code " + code);
    for (final Project project : projects) {

//
//      final String result = ApplicationManager.getApplication().runReadAction(new Computable<String>() {
//        public String compute() {
//          final StringBuilder sb = new StringBuilder();
//          final PsiClass aClass = JavaPsiFacadeEx.getInstanceEx(project).findClass(fqcn);
//
//          final JavaRecursiveElementVisitor javaRecursiveElementVisitor = new JavaRecursiveElementVisitor() {
//            @Override
//            public void visitReferenceElement(PsiJavaCodeReferenceElement reference) {
//              super.visitReferenceElement(reference);
//              if (reference.getContext() instanceof PsiMethodCallExpression) {
//                PsiMethodCallExpression mc = (PsiMethodCallExpression) reference.getContext();
//                final String caller;
//                if (((PsiReferenceExpression) mc.getFirstChild()).resolve() != null) {
//                  final PsiMethod resolve = (PsiMethod) ((PsiReferenceExpression) mc.getFirstChild()).resolve();
//                  if (resolve != null) {
//                    final PsiClass containingClass = resolve.getContainingClass();
//                    if (containingClass != null) {
//                      caller = StringUtils.defaultString(containingClass.getQualifiedName(), "");
//                      if (caller.contains("Logger")) {
//                        sb.append("\nCaller is ").append(caller);
//                        sb.append("\nText: ").append(mc.getText());
//                        final List<PsiLiteralExpression> psiLiteralExpressions = literalExpression(mc.getArgumentList().getExpressions());
//                        for (PsiLiteralExpression psiLiteralExpression : psiLiteralExpressions) {
//                          sb.append("\n");
//                          String text = unwrap(psiLiteralExpression.getText());
//                          if (code.contains(text)) {
//                            final int textOffset = mc.getTextOffset();
//                            final int textLength = mc.getTextLength();
//                            sb.append("\nHIT!: ")
//                                .append(mc.getContainingFile()).append(" from ")
//                                .append(textOffset)
//                                .append(" with length ")
//                                .append(textLength)
//                                .append("\n");
//                            //TODO Jump to this location
//                            final PsiFile containingFile = aClass.getContainingFile();
//                            FileUtils.jumpToLocation(containingFile,textOffset,textLength);
//
//
//                          }
//                        }
//                      }
//                    }
//                  }
//                }
//              }
//            }
//          };
//
//          javaRecursiveElementVisitor.visitElement(aClass);
////          sb.append("\nHave ").append(classesByName.length).append(" results for ").append(project.getName());
////          for (PsiClass psiClass : classesByName) {
////            final PsiField[] allFields = psiClass.getAllFields();
////            sb.append("\nHave ").append(allFields.length).append(" fields");
////            for (PsiField field : allFields) {
////              sb.append("\n").append(field.getName()).append(": ").append(field.getType().getCanonicalText());
////            }
////          }
//          return sb.toString();
//        }
//      });
//      writer.println(result);

    }


  }



  private void content(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String packageName = getParameter(request, "p", "packageName");
    String fileName = getParameter(request, "f", "fileName");
    String className = getParameter(request, "c", "className");
    int lineNumber = parseInt(getParameter(request, "l", "lineNumber"), 0);
    String project = request.getParameter("project");
    String module = request.getParameter("module");
    SourceLocation location;
    if (packageName != null && fileName != null) {
      location = new SourceLocation(packageName, fileName, lineNumber, project, module);
    } else {
      if (className != null) {
        location = new SourceLocation(className);
      } else {
        error(response, "either (packageName,fileName) or (className) is required");
        return;
      }
    }
    String content = FileUtils.getContent(location);

    if (content.length() > 0) {
      response.setStatus(HttpServletResponse.SC_OK);
      response.addHeader("line", Integer.toString(location.getLineNumber()));
      response.getWriter().append(content);
    } else {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  private void jump(HttpServletRequest request, HttpServletResponse response, boolean test) throws IOException {
//    String packageName = getParameter(request, "p", "packageName");
//    String fileName = getParameter(request, "f", "fileName");
//    String className = getParameter(request, "c", "className");
//    int lineNumber = parseInt(getParameter(request, "l", "lineNumber"), 0);
//    String project = request.getParameter("project");
//    String module = request.getParameter("module");
//    SourceLocation location;



    final Optional<String> pkg = getOptParameter(request, "p", "packageName");
    final Optional<String> clazz = getOptParameter(request, "c", "className");
    final Optional<String> file = getOptParameter(request, "f", "file");
    final Optional<String> line = getOptParameter(request, "l", "lineNumber");
    final Optional<String> msg = getOptParameter(request, "m", "message");
    final List<JumpLocation> locations = FileUtils.findLocation(pkg, clazz, file, line, msg);

    if (locations.isEmpty()) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND,"Class not found");
      return;
    }

    boolean ok;
    if (test) {
      ok = !locations.isEmpty();
    } else {
      ok = FileUtils.jumpToLocation(locations);
    }
    if (ok) {
      response.setStatus(HttpServletResponse.SC_OK);
      if (test) {
        response.getWriter().println("OK, found " );
      } else {
        response.getWriter().println("OK, jumped to ");
      }
    } else {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      response.getWriter().println("Not found: ");
    }
  }

  private void form(HttpServletResponse response) throws IOException {
    final InputStream formIs = getClass().getClassLoader().getResourceAsStream("form.html");
    FileCopyUtils.copy(formIs, response.getOutputStream());
    response.setStatus(HttpServletResponse.SC_OK);
  }

  private int parseInt(String value, int defaultValue) {
    if (value == null) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  private void error(HttpServletResponse response, String message) throws IOException {
    response.getWriter().println(message);
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
  }
}
