package pl.otros.intellij.jumptocode.model;

import com.google.common.base.Optional;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethodCallExpression;

public class PsiModelLocation implements JumpLocation {
  private final PsiFile containingFile;
  private final int textOffset;
  private final int textLength;
  private final PsiMethodCallExpression mc;
  private final Optional<PsiElement> parent;


  public PsiModelLocation(PsiFile containingFile, int textOffset, int textLength, PsiMethodCallExpression mc, Optional<PsiElement> parent) {

    this.containingFile = containingFile;
    this.textOffset = textOffset;
    this.textLength = textLength;
    this.mc = mc;
    this.parent = parent;
  }

  public PsiFile getContainingFile() {
    return containingFile;
  }

  public int getTextOffset() {
    return textOffset;
  }

  public int getTextLength() {
    return textLength;
  }

  public PsiMethodCallExpression getMc() {
    return mc;
  }

  public Optional<PsiElement> getParent() {
    return parent;
  }
}
