package pl.otros.intellij.JumpToCode.logic;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import pl.otros.intellij.JumpToCode.gui.SwingUtils;

class RemoveHighLighter implements Runnable {
  private Editor editor;
  private RangeHighlighter highlighter;

  public RemoveHighLighter(Editor editor, RangeHighlighter highlighter) {
    this.editor = editor;
    this.highlighter = highlighter;
  }

  public void run() {
    SwingUtils.invokeSwing(new Runnable() {
      public void run() {
        editor.getMarkupModel().removeHighlighter(highlighter);
      }
    }, false);
  }
}
