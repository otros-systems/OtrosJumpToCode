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

package pl.otros.intellij.jumptocode.server;

import com.google.common.base.Joiner;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.Extensions;
import org.apache.commons.lang.StringUtils;
import pl.otros.intellij.jumptocode.extension.LocatorProvider;
import pl.otros.intellij.jumptocode.logic.FileCopyUtils;
import pl.otros.intellij.jumptocode.logic.FileUtils;
import pl.otros.intellij.jumptocode.logic.locator.JavaFileWithLineLocator;
import pl.otros.intellij.jumptocode.logic.locator.JavaPisLocator;
import pl.otros.intellij.jumptocode.logic.locator.LocationInfo;
import pl.otros.intellij.jumptocode.logic.locator.Locator;
import pl.otros.intellij.jumptocode.model.JumpLocation;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 */
public class JumpToCodeServlet extends HttpServlet {

  public static final Logger LOGGER = PluginManager.getLogger();

  final List<Locator> buildInLocators = Arrays.asList(new JavaPisLocator(), new JavaFileWithLineLocator());
  private String version;
  private String pluginFeatures = Joiner.on(",").join(
      "jumpByLine",
      "jumpByMessage",
      "contentByLine",
      "contentByMessage",
      "allFile"
  );

  public JumpToCodeServlet(String version) {
    this.version = version;
  }

  public List<Locator> getInternalLocaotrs(){
    ArrayList<Locator> r = new ArrayList<Locator>();
    r.addAll(buildInLocators);
    return r;
  }

  public  List<Locator> getLocators(){
    final ArrayList<Locator> locators = new ArrayList<Locator>();
    locators.addAll(getInternalLocaotrs());
    locators.addAll(getLocatorsFromExtensions());
    return locators;
  }

  private List<Locator> getLocatorsFromExtensions() {
    ArrayList<Locator> r = new ArrayList<Locator>();
    final ExtensionPointName<LocatorProvider> extensionPointName = new ExtensionPointName<LocatorProvider>("pl.otros.intellij.JumpToCode.locatorProvider");
    final LocatorProvider[] locatorProviders = Extensions.getExtensions(extensionPointName);
    LOGGER.info("Have " + locatorProviders.length + " locator providers from extensions");
    for (LocatorProvider locatorProvider : locatorProviders) {
      try {
        final Locator locator = locatorProvider.locator();
        LOGGER.info("Adding locator " + locator.getClass().getName());
        r.add(locator);
      } catch (Throwable e){
        LOGGER.error("Can't add locator from " + locatorProvider.getClass().getName() + ": " + e.getMessage(),e);
      }
    }
    return r;
  }


  private static String getParameter(HttpServletRequest request, String shortName, String longName) {
    String value = request.getParameter(longName);
    if (value == null) {
      value = request.getParameter(shortName);
    }
    return StringUtils.defaultString(value, "");
  }

  private static String getParameter(HttpServletRequest request, String shortName, String longName, String defaultValue) {
    String value = getParameter(request, shortName, longName);
    return (value != null) ? value : defaultValue;
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    response.addHeader("ide", "idea");
    response.addHeader("plugin-version", version);
    response.addHeader("plugin-features", pluginFeatures);
    String operation = getParameter(request, "operation", "o", "form");
    if ("form".equals(operation)) {
      response.setContentType("text/html");
      form(response);
      return;
    }


    response.setContentType("text/plain");
    if (StringUtils.equalsIgnoreCase("jump", operation)) {
      jump(request, response, false);
    } else if (StringUtils.equalsIgnoreCase("test", operation)) {
      jump(request, response, true);
    } else if (StringUtils.equalsIgnoreCase("content", operation)) {
      content(request, response);
    } else if (StringUtils.equalsIgnoreCase("all", operation)) {
      final String wholeClass = new FileUtils(getLocators()).findWholeClass(getParameter(request, "c", "className"));
      if (wholeClass.length() > 0) {
        response.getWriter().print(wholeClass);
      } else {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
      }
    } else {
      error(response, "Unexpected operation");
    }
  }

  private void content(HttpServletRequest request, HttpServletResponse response) throws IOException {
    final LocationInfo locationInfo = LocationInfo.parse(request);

    List<String> contents = new FileUtils(getLocators()).getContent(locationInfo);

    if (contents.size() > 0) {
      response.setStatus(HttpServletResponse.SC_OK);
      response.getWriter().append(Joiner.on("\n-----------------\n").join(contents));
    } else {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  private void jump(HttpServletRequest request, HttpServletResponse response, boolean test) throws IOException {
    final LocationInfo locationInfo = LocationInfo.parse(request);
    final List<JumpLocation> locations = new FileUtils(getLocators()).findLocation(locationInfo);

    if (locations.isEmpty()) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND, "Class not found");
      return;
    }

    boolean ok;
    if (test) {
      ok = !locations.isEmpty();
    } else {
      ok = new FileUtils(getLocators()).jumpToLocation(locations);
    }
    if (ok) {
      response.setStatus(HttpServletResponse.SC_OK);
      if (test) {
        response.getWriter().println("OK, found ");
      } else {
        response.getWriter().println("OK, jumped to ");
      }
    } else {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      response.getWriter().println("Not found: ");
    }
  }

  private void form(HttpServletResponse response) throws IOException {
    final InputStream formIs = getClass().getClassLoader().getResourceAsStream("form.html");
    FileCopyUtils.copy(formIs, response.getOutputStream());
    response.setStatus(HttpServletResponse.SC_OK);
  }

  private void error(HttpServletResponse response, String message) throws IOException {
    response.getWriter().println(message);
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
  }
}
