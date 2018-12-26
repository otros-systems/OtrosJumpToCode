package pl.otros.intellij.jumptocode.logic;

import java.util.concurrent.TimeUnit;

import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import org.jetbrains.annotations.NotNull;

class ToRangeCodeJumper extends WriteAction<Boolean> {
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

    @Override
    protected void run(@NotNull Result<Boolean> result) throws Throwable {
        Editor editor = fileEditorManager.openTextEditor(ofd, true);
        if (editor != null && offset >= 0) {
            final TextAttributesKey searchResultAttributes = EditorColors.SEARCH_RESULT_ATTRIBUTES;
            final TextAttributes attributes = searchResultAttributes.getDefaultAttributes();
            RangeHighlighter highlighter = editor.getMarkupModel()
                    .addRangeHighlighter(offset,
                            offset + length,
                            HighlighterLayer.SELECTION,
                            attributes,
                            HighlighterTargetArea.LINES_IN_RANGE);

            FileUtils.scheduledExecutorService.schedule(new RemoveHighLighter(editor, highlighter), 5, TimeUnit.SECONDS);
            result.setResult(true);
        } else {
            result.setResult(false);
        }
    }
}
