package pl.otros.intellij.JumpToCode.logic;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

public class SourceFile {
  VirtualFile virtualFile;
  private Project project;

  public SourceFile(Project project, VirtualFile virtualFile) {
    this.project = project;
    this.virtualFile = virtualFile;
  }


  public VirtualFile getVirtualFile() {
    return virtualFile;
  }

  public Project getProject() {
    return project;
  }
}
