package pl.otros.intellij.JumpToCode.logic;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;

import java.util.concurrent.TimeUnit;

class ToRangeCodeJumper implements Runnable {
  private boolean ok = false;
  private FileEditorManager fileEditorManager;
  private OpenFileDescriptor ofd;
  private int offset;
  private int length;


  ToRangeCodeJumper(FileEditorManager fileEditorManager, OpenFileDescriptor ofd, int offset, int length) {
    this.fileEditorManager = fileEditorManager;
    this.ofd = ofd;
    this.offset = offset;
    this.length = length;
  }

  public void run() {
    Editor editor = fileEditorManager.openTextEditor(ofd, true);
    if (editor != null && offset >= 0) {
      final TextAttributesKey searchResultAttributes = EditorColors.SEARCH_RESULT_ATTRIBUTES;
      final TextAttributes attributes = searchResultAttributes.getDefaultAttributes();
      RangeHighlighter highlighter = editor.getMarkupModel().addRangeHighlighter(offset, offset + length, HighlighterLayer.SELECTION, attributes, HighlighterTargetArea.LINES_IN_RANGE);

      FileUtils.scheduledExecutorService.schedule(new RemoveHighLighter(editor, highlighter), 5, TimeUnit.SECONDS);
      ok = true;
    }
  }

  public boolean isOk() {
    return ok;
  }
}
