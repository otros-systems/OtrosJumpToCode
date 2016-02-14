package pl.otros.intellij.JumpToCode.model;

import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethodCallExpression;

public class PsiModelLocation implements JumpLocation {
  private final PsiFile containingFile;
  private final int textOffset;
  private final int textLength;
  private final PsiMethodCallExpression mc;

  public PsiModelLocation(PsiFile containingFile, int textOffset, int textLength, PsiMethodCallExpression mc) {
    this.containingFile = containingFile;
    this.textOffset = textOffset;
    this.textLength = textLength;
    this.mc = mc;
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
}
