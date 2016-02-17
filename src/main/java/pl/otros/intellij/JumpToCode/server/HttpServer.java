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

package pl.otros.intellij.JumpToCode.server;

import org.apache.log4j.Logger;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import pl.otros.intellij.JumpToCode.logic.ServerConfig;

/**
 */
public class HttpServer {

  private static HttpServer instance = new HttpServer();
  private final Logger logger = Logger.getLogger(this.getClass());
  private Server server;

  private HttpServer() {
  }

  public static HttpServer getInstance() {
    return instance;
  }

  public boolean isActive() {
    return (server != null) && server.isRunning();
  }

  synchronized public void configure(ServerConfig config) {
    if (isActive()) {
      stop();
    }
    if (config.isEnabled()) {
      start(config);
    }
  }

  private void stop() {
    try {
      server.stop();
      logger.warn("stopped JumpToCode HTTP server");
    } catch (Exception e) {
      logger.error("failed to stop JumpToCode HTTP server", e);
    }
  }

  private void start(ServerConfig config) {
    logger.debug("starting HttpServer");
    server = new Server();
    Connector connector = new SocketConnector();
    connector.setPort(config.getPortNumber());
    connector.setHost(config.getHostName());
    server.setConnectors(new Connector[]{connector});
    Context root = new Context(server, "/", Context.NO_SESSIONS);
    //TODO replace with value read from plugin.xml
    root.addServlet(new ServletHolder(new JumpToCodeServlet("1.4")), "/*");
    server.setStopAtShutdown(true);
    try {
      server.start();
      logger.info("started JumpToCode HTTP server at "
          + config.getHostName()
          + ":"
          + config.getPortNumber());
    } catch (Exception e) {
      logger.error("failed to start JumpToCode HTTP server", e);
    }
  }
}

