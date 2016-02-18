package pl.otros.intellij.jumptocode.logic;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.PackageIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import pl.otros.intellij.jumptocode.model.SourceLocation;

import java.util.ArrayList;
import java.util.List;

public class SourceFileFinder {
  public List<SourceFile> findSourceFiles(SourceLocation location) {
    ProjectManager projectManager = ProjectManager.getInstance();
    Project[] projects = projectManager.getOpenProjects();
    List<SourceFile> matches = new ArrayList<SourceFile>();
    for (Project project : projects) {
      ProjectRootManager prm = ProjectRootManager.getInstance(project);
      PackageIndex packageIndex = PackageIndex.getInstance(project);
      VirtualFile[] dirs = packageIndex.getDirectoriesByPackageName(location.getPackageName(), true);
      for (VirtualFile vf : dirs) {
        VirtualFile child = vf.findChild(location.getFileName());
        if (child != null) {
          SourceFile file = new SourceFile(project, child);
          matches.add(file);
        }
      }
    }
    return matches;
  }
}
