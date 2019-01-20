package pl.otros.intellij.jumptocode.logic;

import java.util.concurrent.TimeUnit;

import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import org.jetbrains.annotations.NotNull;

class ToLineCodeJumper extends WriteAction<Boolean> {
    private FileEditorManager fileEditorManager;
    private OpenFileDescriptor ofd;
    private int lineNumber;


    ToLineCodeJumper(FileEditorManager fileEditorManager, OpenFileDescriptor ofd, int lineNumber) {
        this.fileEditorManager = fileEditorManager;
        this.ofd = ofd;
        this.lineNumber = lineNumber;
    }


    @Override
    protected void run(@NotNull Result<Boolean> result) {
        Editor editor = fileEditorManager.openTextEditor(ofd, true);
        if (editor != null && lineNumber >= 0) {
            final TextAttributesKey searchResultAttributes = EditorColors.SEARCH_RESULT_ATTRIBUTES;
            final TextAttributes attributes = searchResultAttributes.getDefaultAttributes();
            RangeHighlighter highlighter = editor.getMarkupModel()
                    .addLineHighlighter(lineNumber, HighlighterLayer.ERROR, attributes);
            FileUtils.scheduledExecutorService.schedule(new RemoveHighLighter(editor, highlighter), 5, TimeUnit.SECONDS);
            result.setResult(true);
        } else {
            result.setResult(false);
        }
    }
}
