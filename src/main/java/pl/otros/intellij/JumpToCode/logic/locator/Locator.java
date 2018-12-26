package pl.otros.intellij.jumptocode.logic.locator;

import java.util.List;

import pl.otros.intellij.jumptocode.model.JumpLocation;

public interface Locator {

  List<? extends JumpLocation> findLocation(LocationInfo locationInfo);
  List<String> getContent(LocationInfo locationInfo);

  String name();

}
