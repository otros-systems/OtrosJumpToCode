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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.*;
import com.intellij.psi.impl.JavaPsiFacadeEx;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import org.apache.commons.lang.StringUtils;
import pl.otros.intellij.JumpToCode.logic.FileCopyUtils;
import pl.otros.intellij.JumpToCode.logic.FileUtils;
import pl.otros.intellij.JumpToCode.model.SourceLocation;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

/**
 */
public class JumpToCodeServlet extends HttpServlet {
  private static String getParameter(HttpServletRequest request, String shortName, String longName) {
    String value = request.getParameter(longName);
    if (value == null) {
      value = request.getParameter(shortName);
    }
    return value;
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
    } else if (StringUtils.endsWithIgnoreCase("new", operation)) {
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
    String fileName = getParameter(request, "f", "fileName");
    final String className = getParameter(request, "c", "className");
    int lineNumber = parseInt(getParameter(request, "l", "lineNumber"), 0);
//    String project = request.getParameter("project");
    String module = request.getParameter("module");

    ProjectManager projectManager = ProjectManager.getInstance();
    Project[] projects = projectManager.getOpenProjects();
    final String fqcn = packageName + "." + className;
    final PrintWriter writer = response.getWriter();
        //System.out;
    response.setContentType("text/plain");
    writer.println("Response for " + fqcn);
    for (final Project project : projects) {


      final String result = ApplicationManager.getApplication().runReadAction(new Computable<String>() {
        public String compute() {
          final StringBuilder sb = new StringBuilder();
          final GlobalSearchScope scope = GlobalSearchScope.allScope(project);
          final PsiClass[] classesByName = PsiShortNamesCache.getInstance(project).getClassesByName(className, scope);
          final PsiClass aClass = JavaPsiFacadeEx.getInstanceEx(project).findClass(fqcn);

          final JavaRecursiveElementVisitor javaRecursiveElementVisitor = new JavaRecursiveElementVisitor() {
            @Override
            public void visitReferenceElement(PsiJavaCodeReferenceElement reference) {
              super.visitReferenceElement(reference);
              sb.append("\n");
              if (reference.getContext() instanceof PsiMethodCallExpression) {
                PsiMethodCallExpression mc = (PsiMethodCallExpression) reference.getContext();
                sb.append(mc.getMethodExpression().getReferenceName());
                sb.append(mc.getFirstChild());
                final String caller = ((PsiMethod) ((PsiReferenceExpression) mc.getFirstChild()).resolve()).getContainingClass().getQualifiedName();
                sb.append("\nCaller is ").append(caller);
                sb.append("\nText: ").append(mc.getText());
              }
            }
          };

          javaRecursiveElementVisitor.visitElement(aClass);
          sb.append("\nHave ").append(classesByName.length).append(" results for ").append(project.getName());
          for (PsiClass psiClass : classesByName) {
            final PsiField[] allFields = psiClass.getAllFields();
            sb.append("\nHave ").append(allFields.length).append(" fields");
            for (PsiField field : allFields) {
              sb.append("\n").append(field.getName()).append(": ").append(field.getType().getCanonicalText());
            }
          }
          return sb.toString();
        }
      });
      writer.println(result);

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
    boolean ok;
    if (test) {
      ok = FileUtils.isReachable(location);
    } else {
      ok = FileUtils.jumpToLocation(location);
    }
    if (ok) {
      response.setStatus(HttpServletResponse.SC_OK);
      if (test) {
        response.getWriter().println("OK, found " + location);
      } else {
        response.getWriter().println("OK, jumped to " + location);
      }
    } else {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      response.getWriter().println("Not found: " + location);
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
