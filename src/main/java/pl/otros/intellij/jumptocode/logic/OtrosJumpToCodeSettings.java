package pl.otros.intellij.jumptocode.logic;

import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.components.*;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;
import pl.otros.intellij.jumptocode.server.HttpServer;

import javax.swing.*;


@State(name = "OtrosJumpToCodeSettings",storages = @Storage("OtrosJumpToCode.cfg"))
public class OtrosJumpToCodeSettings implements PersistentStateComponent<Config>, Configurable {

  private static final com.intellij.openapi.diagnostic.Logger LOGGER = PluginManager.getLogger();

  private Config config = Config.getInstance();

  private JumpToCodeConfigurationForm form;

  public static OtrosJumpToCodeSettings getInstance() {
    return ServiceManager.getService(OtrosJumpToCodeSettings.class);
  }

  @Nullable
  @Override
  public Config getState() {
    LOGGER.info("Returning current config: " + config);
    return config;
  }

  public void loadState(Config state) {
    XmlSerializerUtil.copyBean(state, config);
    LOGGER.debug("loadState: config.enabled= " + state.isEnabled());
  }

  @Nls
  @Override
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

  public void apply() {
    if (form != null) {
      LOGGER.info("Applying configuration");
      form.getData(config);
      LOGGER.info("Configuring HTTP server");
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
}
