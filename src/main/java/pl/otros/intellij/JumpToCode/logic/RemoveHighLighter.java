package pl.otros.intellij.jumptocode.logic;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import pl.otros.intellij.jumptocode.gui.SwingUtils;

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
