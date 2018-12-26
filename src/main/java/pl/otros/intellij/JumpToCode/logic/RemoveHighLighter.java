package pl.otros.intellij.jumptocode.logic;

import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import org.jetbrains.annotations.NotNull;

class RemoveHighLighter extends WriteAction<Void> implements Runnable {
  private Editor editor;
  private RangeHighlighter highlighter;

  public RemoveHighLighter(Editor editor, RangeHighlighter highlighter) {
    this.editor = editor;
    this.highlighter = highlighter;
  }

  @Override
  protected void run(@NotNull Result<Void> result) throws Throwable {
      editor.getMarkupModel().removeHighlighter(highlighter);
  }

  @Override
  public void run() {
    this.execute();
  }
}
