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

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.intellij.openapi.extensions.Extensions;
import pl.otros.intellij.jumptocode.Properties;
import pl.otros.intellij.jumptocode.extension.LocatorProvider;
import pl.otros.intellij.jumptocode.gui.DonateNotificationProvider;
import pl.otros.intellij.jumptocode.logic.locator.Locator;
import pl.otros.intellij.jumptocode.server.JumpToCodeServlet;

/**
 */
public class JumpToCodeConfigurationForm {
  private JTextField hostnameField;
  private JPanel rootComponent;
  private JTextField portField;
  private JCheckBox enabledCheckBox;
  private JButton openOtrosLogViewerPageButton;
  private JButton openWikiPageButton;
  private JTextArea jumpCount;
  private JButton donateWithBitCoinButton;
  private JButton donateWithPaypal;
  private JTextArea installedLocatorProviders;

  public JumpToCodeConfigurationForm() {
    openOtrosLogViewerPageButton.addActionListener(new OpenBrowser("http://code.google.com/p/otroslogviewer/"));
    openWikiPageButton.addActionListener(new OpenBrowser("http://code.google.com/p/otroslogviewer/wiki/JumpToCode"));
    donateWithPaypal.addActionListener(new OpenBrowser(DonateNotificationProvider.DONATION_URL));
    donateWithBitCoinButton.addActionListener(new OpenBrowser("https://code.google.com/p/otroslogviewer/wiki/DonateAndDonors"));
  }

  // Method returns the root component of the form
  public JComponent getRootComponent() {
    return rootComponent;
  }

  public void setData(Config data) {
    portField.setText(data.getPort());
    enabledCheckBox.setSelected(data.isEnabled());
    hostnameField.setText(data.getHostName());
    jumpCount.setText(String.format("You have performed %d jumps from OtrosLogViewer", Properties.getJumpsCount()));

    List<LocatorProvider> locatorProviders = Arrays
              .asList(Extensions.getExtensions("pl.otros.intellij.JumpToCode.locatorProvider"))
              .stream()
              .filter(o -> o instanceof LocatorProvider)
              .map(o -> (LocatorProvider) o)
              .collect(Collectors.toList());
    final List<Locator> locators = new ArrayList<>(JumpToCodeServlet.buildInLocators);
    for (LocatorProvider locatorProvider : locatorProviders) {
      locators.add(locatorProvider.locator());
    }
    StringBuilder sb = new StringBuilder("Installed source code locators:\n");
    for (Locator locator : locators) {
      sb.append(" - ")
          .append(locator.name())
          .append("\n");
    }
    installedLocatorProviders.setText(sb.toString());
  }

  public void getData(Config data) {
    data.setPort(portField.getText());
    data.setEnabled(enabledCheckBox.isSelected());
    data.setHostName(hostnameField.getText());
  }

  @SuppressWarnings({"RedundantIfStatement"})
  public boolean isModified(Config data) {
    if (portField.getText() != null ? !portField.getText().equals(data.getPort()) : data.getPort() != null) {
      return true;
    }
    if (enabledCheckBox.isSelected() != data.isEnabled()) {
      return true;
    }
    return hostnameField.getText() != null ? !hostnameField.getText().equals(data.getHostName()) : data.getHostName() != null;
  }


  private static class OpenBrowser implements ActionListener {

    private String url;

    public OpenBrowser(String url) {
      this.url = url;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      try {
        Desktop.getDesktop().browse(URI.create(url));
      } catch (IOException e1) {
        e1.printStackTrace();
      }
    }
  }

}
