package pl.otros.intellij.jumptocode.logic.locator;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.io.ByteStreams;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.JavaPsiFacadeEx;
import com.intellij.psi.impl.source.PsiClassImpl;
import com.intellij.psi.impl.source.PsiMethodImpl;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import pl.otros.intellij.jumptocode.model.JumpLocation;
import pl.otros.intellij.jumptocode.model.PsiModelLocation;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

public class JavaPisLocator implements Locator {

  @Override
  public List<PsiModelLocation> findLocation(LocationInfo locationInfo) {
    if (locationInfo.clazz.isPresent() && locationInfo.msg.isPresent()) {
      return findByLogMessage(locationInfo.clazz.get(), locationInfo.msg.get());
    }
    return Collections.emptyList();
  }

  @Override
  public List<String> getContent(LocationInfo locationInfo) {
    if (locationInfo.clazz.isPresent() && locationInfo.msg.isPresent()) {
      return getContentByMessage(locationInfo.clazz.get(), locationInfo.msg.get());
    }
    return Collections.emptyList();
  }


  public List<String> getContentByMessage(final String fqcn, final String code) {
    final ArrayList<String> list = new ArrayList<String>();
    final List<? extends JumpLocation> locations = findByLogMessage(fqcn, code);
    for (JumpLocation location : locations)
      if (location instanceof PsiModelLocation) {
        final PsiModelLocation psiModelLocation = (PsiModelLocation) location;
        final VirtualFile virtualFile = psiModelLocation.getContainingFile().getContainingFile().getVirtualFile();
        try {
          final String content = new String(virtualFile.contentsToByteArray());
          final Function<PsiElement, Integer> psiElementToOffset = new PsiElementIntegerFunction();
          final int methodCallStart = psiModelLocation.getTextOffset();
          int start;
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
          final String[] lines = content.substring(start, end).split("\n");
          int maxLines = 15;
          String toDisplay = "\nPath: " +
              ((PsiModelLocation) location).getContainingFile().getVirtualFile().getCanonicalPath() +
              "\n" +
              Joiner.on("\n").join(abbreviateLines(lines, maxLines, 3));
          list.add(toDisplay);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    return list;
  }

  private String readVirtualFile(VirtualFile virtualFile) {
    try {
      final byte[] bytes = ByteStreams.toByteArray(virtualFile.getInputStream());
      return new String(bytes, Charset.forName("UTF-8"));
    } catch (IOException e) {
      e.printStackTrace();
    }
    return "";
  }

  private List<PsiModelLocation> findByLogMessage(final String fqcn, final String code) {
    final ArrayList<PsiModelLocation> jumpLocations = new ArrayList<PsiModelLocation>();

    ProjectManager projectManager = ProjectManager.getInstance();
    Project[] projects = projectManager.getOpenProjects();

    for (final Project project : projects) {

      final List<PsiModelLocation> result = ApplicationManager.getApplication().runReadAction(new Computable<List<PsiModelLocation>>() {
        public List<PsiModelLocation> compute() {
          final PsiClass aClass = JavaPsiFacadeEx.getInstanceEx(project).findClass(fqcn);
          final ArrayList<PsiModelLocation> result = new ArrayList<PsiModelLocation>();
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
                          String text = psiLiteralExpression.getText();
                          text = unwrap(text);
                          text = StringEscapeUtils.unescapeJava(text);
                          text = withoutArgsPlaceHolders(text);
                          if (code.contains(text)) {
                            final int textOffset = mc.getTextOffset();
                            final int textLength = mc.getTextLength();
                            final PsiFile containingFile;
                            if (aClass != null) {
                              containingFile = aClass.getContainingFile();
                              result.add(new PsiModelLocation(containingFile, textOffset, textLength, mc, extractParent(mc)));
                            }
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

  private static List<PsiLiteralExpression> literalExpression(PsiExpression[] expressions) {
    List<PsiLiteralExpression> r = new ArrayList<PsiLiteralExpression>();
    for (PsiExpression e : expressions) {
      if (e instanceof PsiMethodCallExpression){
        final PsiExpressionList argumentList = ((PsiMethodCallExpression) e).getArgumentList();
        r.addAll(literalExpression(argumentList.getExpressions()));
      }else if (e instanceof PsiLiteralExpression) {
        r.add((PsiLiteralExpression) e);
      } else if (e instanceof PsiBinaryExpression) {
        //logger.info("message",param);
        PsiBinaryExpression be = (PsiBinaryExpression) e;
        r.addAll(literalExpression(be.getOperands()));
      } else if (e instanceof PsiPolyadicExpression) {
        //logger.info("String " + something + " something")
        r.addAll(literalExpression(((PsiPolyadicExpression) e).getOperands()));
      }
    }
    return r;
  }

  protected String unwrap(String text) {
    if (text.startsWith("\"") && text.endsWith("\"") && text.length() > 2) {
      return text.substring(1, text.length() - 1);
    } else {
      return text;
    }
  }

  protected String withoutArgsPlaceHolders(String text) {
    String[] splited = text.split("(\\{\\})");
    final Comparator<String> stringComparator = new Comparator<String>() {
      @Override
      public int compare(String o1, String o2) {
        return o2.length() - o1.length();
      }
    };
    Arrays.sort(splited, stringComparator);
    final String s1 = splited[0];
    String s = s1.replaceFirst("^\\s*%\\S+","");
    return StringUtils.substringBefore(s, "%");
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

  protected String[] abbreviateLines(String[] lines, int max, int header){
    if (lines.length <= max || lines.length<10) {
      return lines;
    } else {
      final ArrayList<String> strings = new ArrayList<String>();
      String[] result = new String[max];
      System.arraycopy(lines,0,result,0,header);
      String spaces = StringUtils.getCommonPrefix(new String[]{"                        ",lines[header-1]});

      result[header] = spaces + ".........";
      result[header+1] = result[header];
      final int tail = max - header - 2;
      System.arraycopy(lines,lines.length- tail,result,header+2,tail);
      return result;
    }
  }

  private static class PsiElementIntegerFunction implements Function<PsiElement, Integer> {
    @Override
    public Integer apply(PsiElement psiElement) {
      return psiElement.getTextOffset();
    }
  }

}
