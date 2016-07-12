/*
 * Copyright 2014 otros.systems@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pl.otros.intellij.jumptocode.logic;

import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.components.ApplicationComponent;
import org.jetbrains.annotations.NotNull;
import pl.otros.intellij.jumptocode.server.HttpServer;


public class OtrosJumpToCodeApplicationComponent implements ApplicationComponent {

  public static final com.intellij.openapi.diagnostic.Logger LOGGER = PluginManager.getLogger();


  public OtrosJumpToCodeApplicationComponent() {
    final OtrosJumpToCodeSettings instance = OtrosJumpToCodeSettings.getInstance();
    final Config config = instance.getState();
    LOGGER.debug("constructed OtrosJumpToCodeApplicationComponent");
    ToggleAction toggleAction = new ToggleAction("OtrosJumpToCode: Enable") {


      @Override
      public boolean isSelected(AnActionEvent anActionEvent) {
        return config.isEnabled();
      }

      @Override
      public void setSelected(AnActionEvent anActionEvent, boolean b) {
        LOGGER.info("Setting enabled OtrosJumpToCode: " + b);
        config.setEnabled(b);
        HttpServer.getInstance().configure(config);

      }

      @Override
      public void update(@NotNull AnActionEvent e) {
        super.update(e);
        final boolean selected = this.isSelected(e);
        LOGGER.info("Updating, selected: " + selected);
        e.getPresentation().setText("OtrosJumpToCode enabled: " + config.isEnabled());
      }
    };

    ActionManager.getInstance().registerAction("OtrosJumpToCode-toggle", toggleAction);
  }

  @Override
  public void initComponent() {
    LOGGER.debug("OtrosJumpToCodeApplicationComponent.initComponent");
    HttpServer.getInstance().configure(OtrosJumpToCodeSettings.getInstance().getState());
  }

  @Override
  public void disposeComponent() {
    //Nothing to do
  }

  @NotNull
  public String getComponentName() {
    return "OtrosJumpToCodeApplicationComponent";
  }

}
