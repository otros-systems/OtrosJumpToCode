package pl.otros.intellij.JumpToCode.logic;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

class SourceFile {
  Module module;
  VirtualFile virtualFile;
  private Project project;

  SourceFile(Project project, Module module, VirtualFile virtualFile) {
    this.project = project;
    this.module = module;
    this.virtualFile = virtualFile;
  }

  @Override
  public String toString() {
    String moduleName = (module != null) ? module.getName() : "null";
    String projectName = (project != null) ? project.getName() : "null";
    return String.format("project=[%s] module=[%s] path=[%s}",
        projectName, moduleName, virtualFile.getPath());
  }

  public Module getModule() {
    return module;
  }

  public VirtualFile getVirtualFile() {
    return virtualFile;
  }

  public Project getProject() {
    return project;
  }
}
