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

package pl.otros.intellij.JumpToCode.logic;

import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.components.*;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.otros.intellij.JumpToCode.server.HttpServer;

import javax.swing.*;

@State(
    name = "OtrosJumpToCodeApplicationComponent",
    storages = {@Storage(id = "OtrosJumpToCode", file = StoragePathMacros.APP_CONFIG + "/OtrosJumpToCode.xml")}
)
public class OtrosJumpToCodeApplicationComponent implements ApplicationComponent, Configurable,
    PersistentStateComponent<Config> {

  static final com.intellij.openapi.diagnostic.Logger LOGGER = PluginManager.getLogger();

  private Config config = new Config();

  private JumpToCodeConfigurationForm form;

  public OtrosJumpToCodeApplicationComponent() {
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

  @Nls
  public String getDisplayName() {
    return "OtrosJumpToCode";
  }


  @Nullable
  @NonNls
  public String getHelpTopic() {
    return null;
  }

  public JComponent createComponent() {
    if (form == null) {
      form = new JumpToCodeConfigurationForm();
    }
    return form.getRootComponent();
  }

  public boolean isModified() {
    return form != null && form.isModified(this.config);
  }

  public void apply() throws ConfigurationException {
    if (form != null) {
      form.getData(config);
      HttpServer.getInstance().configure(config);
    }
  }

  public void reset() {
    if (form != null) {
      form.setData(this.config);
    }
  }

  public void disposeUIResources() {
    form = null;
  }

  public void initComponent() {
    LOGGER.debug("OtrosJumpToCodeApplicationComponent.initComponent");
    HttpServer.getInstance().configure(config);
  }

  public void disposeComponent() {
  }

  @NotNull
  public String getComponentName() {
    return "OtrosJumpToCodeApplicationComponent";
  }

  public Config getState() {
    return config;
  }

  public void loadState(Config state) {
    XmlSerializerUtil.copyBean(state, config);
    LOGGER.debug("loadState: config.enabled= " + state.isEnabled());
  }

}
