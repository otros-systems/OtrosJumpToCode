package pl.otros.intellij.jumptocode.logic;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;

import java.util.concurrent.TimeUnit;

class ToLineCodeJumper implements Runnable {
  private boolean ok = false;
  private FileEditorManager fileEditorManager;
  private OpenFileDescriptor ofd;
  private int lineNumber;


  ToLineCodeJumper(FileEditorManager fileEditorManager, OpenFileDescriptor ofd, int lineNumber) {
    this.fileEditorManager = fileEditorManager;
    this.ofd = ofd;
    this.lineNumber = lineNumber;
  }

  public void run() {
    Editor editor = fileEditorManager.openTextEditor(ofd, true);
    if (editor != null && lineNumber >= 0) {
      final TextAttributesKey searchResultAttributes = EditorColors.SEARCH_RESULT_ATTRIBUTES;
      final TextAttributes attributes = searchResultAttributes.getDefaultAttributes();
      RangeHighlighter highlighter = editor.getMarkupModel().addLineHighlighter(lineNumber, HighlighterLayer.ERROR, attributes);
      FileUtils.scheduledExecutorService.schedule(new RemoveHighLighter(editor, highlighter), 5, TimeUnit.SECONDS);
      ok = true;
    }
  }

  public boolean isOk() {
    return ok;
  }
}
