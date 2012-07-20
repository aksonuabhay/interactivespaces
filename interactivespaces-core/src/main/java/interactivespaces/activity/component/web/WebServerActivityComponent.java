/*
 * Copyright (C) 2012 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package interactivespaces.activity.component.web;

import interactivespaces.activity.Activity;
import interactivespaces.activity.component.ActivityComponent;
import interactivespaces.activity.component.ActivityComponentContext;
import interactivespaces.activity.component.BaseActivityComponent;
import interactivespaces.configuration.Configuration;
import interactivespaces.service.web.server.HttpFileUploadListener;
import interactivespaces.service.web.server.WebServer;
import interactivespaces.service.web.server.WebSocketConnection;
import interactivespaces.service.web.server.WebSocketHandler;
import interactivespaces.service.web.server.WebSocketHandlerFactory;
import interactivespaces.service.web.server.internal.netty.NettyWebServer;

import java.io.File;
import java.util.List;

import com.google.common.collect.Lists;

/**
 * An {@link ActivityComponent} which starts up a web server.
 * 
 * @author Keith M. Hughes
 */
public class WebServerActivityComponent extends BaseActivityComponent {

	/**
	 * Name of the component.
	 */
	public static final String COMPONENT_NAME = "web.server";

	/**
	 * Configuration property giving the port the web server should be started
	 * on.
	 */
	public static final String CONFIGURATION_WEBAPP_WEB_SERVER_PORT = "space.activity.webapp.web.server.port";

	/**
	 * Configuration property giving the websocket URI for the web server on.
	 */
	public static final String CONFIGURATION_WEBAPP_WEB_SERVER_WEBSOCKET_URI = "space.activity.webapp.web.server.websocket.uri";

	/**
	 * Configuration property giving location of the webapp content. Relative
	 * paths give relative to app install directory.
	 */
	public static final String CONFIGURATION_WEBAPP_CONTENT_LOCATION = "space.activity.webapp.content.location";

	/**
	 * Default port to give to the web server.
	 */
	public static final int WEB_SERVER_PORT_DEFAULT = 9000;

	/**
	 * URL for the web activity.
	 */
	private String webContentUrl;

	/**
	 * Port the web server will run on.
	 */
	private int webServerPort;

	/**
	 * Web server for the app, if needed.
	 */
	private WebServer webServer;

	/**
	 * The path to the web content. This is the absolute path portion of the
	 * URL.
	 */
	private String webContentPath;

	/**
	 * The base directory of the web content being served.
	 */
	private File webContentBaseDir;

	/**
	 * Factory for web socket handlers.
	 */
	private WebSocketHandlerFactory webSocketHandlerFactory;

	/**
	 * A potential listener for file uploads.
	 */
	private HttpFileUploadListener httpFileUploadListener;

	/**
	 * Prefix of the URI for the web socket connections.
	 */
	private String webSocketUriPrefix;

	/**
	 * List of static content for the web server.
	 */
	private List<StaticContent> staticContent = Lists.newArrayList();

	@Override
	public String getName() {
		return COMPONENT_NAME;
	}

	@Override
	public void configureComponent(Configuration configuration,
			ActivityComponentContext componentContext) {
		super.configureComponent(configuration, componentContext);

		Activity activity = getComponentContext().getActivity();

		webSocketUriPrefix = configuration
				.getPropertyString(CONFIGURATION_WEBAPP_WEB_SERVER_WEBSOCKET_URI);

		webServerPort = configuration.getPropertyInteger(
				CONFIGURATION_WEBAPP_WEB_SERVER_PORT, WEB_SERVER_PORT_DEFAULT);
		webServer = new NettyWebServer(String.format("%sWebServer",
				activity.getName()), webServerPort, activity
				.getSpaceEnvironment().getExecutorService(), activity.getLog());

		webContentPath = "/" + activity.getName();
		webContentUrl = "http://localhost:" + webServer.getPort()
				+ webContentPath;

		String contentLocation = configuration
				.getRequiredPropertyString(CONFIGURATION_WEBAPP_CONTENT_LOCATION);
		webContentBaseDir = new File(activity.getActivityFilesystem()
				.getInstallDirectory(), contentLocation);

		webServer.addStaticContentHandler(webContentPath, webContentBaseDir);

		for (StaticContent content : staticContent) {
			webServer.addStaticContentHandler(content.getUriPrefix(),
					content.getBaseDir());
		}

		if (webSocketHandlerFactory != null) {
			setWebServerWebSocketHandlerFactory();
		}

		if (httpFileUploadListener != null) {
			webServer.setHttpFileUploadListener(httpFileUploadListener);
		}
	}

	@Override
	public void startupComponent() {
		webServer.start();
		getComponentContext().getActivity().getLog()
				.debug("web server component started up");
	}

	@Override
	public void shutdownComponent() {
		if (webServer != null) {
			webServer.shutdown();
			webServer = null;
		}
	}

	@Override
	public boolean isComponentRunning() {
		// TODO(keith): Anything to check on the web server?
		return true;
	}

	/**
	 * @return the webContentUrl
	 */
	public String getWebContentUrl() {
		return webContentUrl;
	}

	/**
	 * @return the webServerPort
	 */
	public int getWebServerPort() {
		return webServerPort;
	}

	/**
	 * @return the webServer
	 */
	public WebServer getWebServer() {
		return webServer;
	}

	/**
	 * @return the webContentPath
	 */
	public String getWebContentPath() {
		return webContentPath;
	}

	/**
	 * @return the webContentBaseDir
	 */
	public File getWebContentBaseDir() {
		return webContentBaseDir;
	}

	/**
	 * Set the web socket handler factory for the web server to use.
	 * 
	 * <p>
	 * This can be called either before or after calling
	 * {@link WebServerActivityComponent#configureComponent(Activity, Configuration)}
	 * . But if called both before and after, the second call will be the one
	 * used.
	 * 
	 * @param webSocketUriPrefix
	 *            the prefix for web socket connections (can be {@code null})
	 * 
	 * @return the web server component this method was called on
	 */
	public WebServerActivityComponent setWebSocketUriPrefix(
			String webSocketUriPrefix) {
		this.webSocketUriPrefix = webSocketUriPrefix;

		return this;
	}

	/**
	 * Set the web socket handler factory for the web server to use.
	 * 
	 * <p>
	 * This can be called either before or after calling
	 * {@link WebServerActivityComponent#configureComponent(Activity, Configuration)}
	 * . But if called both before and after, the second call will be the one
	 * used.
	 * 
	 * @param webSocketHandlerFactory
	 *            the webSocketHandlerFactory to set
	 * 
	 * @return the web server component this method was called on
	 */
	public WebServerActivityComponent setWebSocketHandlerFactory(
			WebSocketHandlerFactory webSocketHandlerFactory) {
		this.webSocketHandlerFactory = webSocketHandlerFactory;

		if (webServer != null) {
			setWebServerWebSocketHandlerFactory();
		}

		return this;
	}

	/**
	 * Set the HTTP file upload listener for the web server to use.
	 * 
	 * <p>
	 * This can be called either before or after calling
	 * {@link WebServerActivityComponent#configureComponent(Activity, Configuration)}
	 * . But if called both before and after, the second call will be the one
	 * used.
	 * 
	 * @param httpFileUploadListener
	 *            the HTTP file upload listener to use (can be {@code null})
	 * 
	 * @return the web server component this method was called on
	 */
	public WebServerActivityComponent setHttpFileUploadListener(
			HttpFileUploadListener httpFileUploadListener) {
		this.httpFileUploadListener = httpFileUploadListener;

		if (webServer != null) {
			webServer.setHttpFileUploadListener(httpFileUploadListener);
		}

		return this;
	}

	/**
	 * Add static content for the web server to serve.
	 * 
	 * <p>
	 * This can be called either before or after calling
	 * {@link WebServerActivityComponent#configureComponent(Activity, Configuration)}
	 * . But if called both before and after, the second call will be the one
	 * used.
	 * 
	 * @param uriPrefix
	 *            the URI prefix for this particular content
	 * @param baseDir
	 *            the base directory where the content will be found
	 * 
	 * @return the web server component this method was called on
	 */
	public WebServerActivityComponent addStaticContent(String uriPrefix,
			File baseDir) {
		if (webServer != null) {
			webServer.addStaticContentHandler(uriPrefix, baseDir);
		} else {
			staticContent.add(new StaticContent(uriPrefix, baseDir));
		}

		return this;
	}

	/**
	 * Set the web server web socket handler with the proper wrapped factory.
	 */
	private void setWebServerWebSocketHandlerFactory() {
		webServer.setWebSocketHandlerFactory(webSocketUriPrefix,
				new MyWebSocketHandlerFactory(webSocketHandlerFactory,
						getComponentContext()));
	}

	/**
	 * Information about static content.
	 * 
	 * @author Keith M. Hughes
	 */
	public static class StaticContent {

		/**
		 * URI prefix where the content will be referenced from.
		 */
		private String uriPrefix;

		/**
		 * Base directory where the content is stored.
		 */
		private File baseDir;

		public StaticContent(String uriPrefix, File baseDir) {
			this.uriPrefix = uriPrefix;
			this.baseDir = baseDir;
		}

		/**
		 * @return the uriPrefix
		 */
		public String getUriPrefix() {
			return uriPrefix;
		}

		/**
		 * @return the baseDir
		 */
		public File getBaseDir() {
			return baseDir;
		}
	}

	/**
	 * A {@link WebSocketHandlerFactory} which delegates to another web socket
	 * handler factory and wraps web socket handler with
	 * {@link MyWebSocketHandler}.
	 * 
	 * @author Keith M. Hughes
	 */
	public static class MyWebSocketHandlerFactory implements
			WebSocketHandlerFactory {
		/**
		 * The factory being delegated to.
		 */
		private WebSocketHandlerFactory delegate;

		/**
		 * The component context this factory is part of.
		 */
		private ActivityComponentContext activityComponentContext;

		public MyWebSocketHandlerFactory(WebSocketHandlerFactory delegate,
				ActivityComponentContext activityComponentContext) {
			this.delegate = delegate;
			this.activityComponentContext = activityComponentContext;
		}

		@Override
		public WebSocketHandler newWebSocketHandler(WebSocketConnection proxy) {
			WebSocketHandler handlerDelegate = delegate
					.newWebSocketHandler(proxy);
			return new MyWebSocketHandler(handlerDelegate,
					activityComponentContext);
		}
	}

	/**
	 * A {@link WebSocketHandler} which delegates to a web socket handler but
	 * ensures that the component is running.
	 * 
	 * @author Keith M. Hughes
	 */
	public static class MyWebSocketHandler implements WebSocketHandler {

		/**
		 * The delegate to be protected.
		 */
		private WebSocketHandler delegate;

		/**
		 * The component context this handler is part of.
		 */
		private ActivityComponentContext activityComponentContext;

		/**
		 * 
		 * @param delegate
		 *            the handler that all methods will be delegated to
		 * @param activityComponentContext
		 *            the context in charge of the component
		 */
		public MyWebSocketHandler(WebSocketHandler delegate,
				ActivityComponentContext activityComponentContext) {
			this.delegate = delegate;
			this.activityComponentContext = activityComponentContext;
		}

		@Override
		public void onConnect() {
			try {
				if (activityComponentContext.lockReadRunningRead()) {
					delegate.onConnect();
				}
			} catch (Exception e) {
				activityComponentContext.getActivity().getLog()
						.error("Error during web socket connection", e);
			} finally {
				activityComponentContext.unlockReadRunningRead();
			}
		}

		@Override
		public void onClose() {
			try {
				if (activityComponentContext.lockReadRunningRead()) {
					delegate.onClose();
				}
			} catch (Exception e) {
				activityComponentContext.getActivity().getLog()
						.error("Error during web socket close", e);
			} finally {
				activityComponentContext.unlockReadRunningRead();
			}
		}

		@Override
		public void onReceive(Object data) {
			try {
				if (activityComponentContext.lockReadRunningRead()) {
					delegate.onReceive(data);
				}
			} catch (Exception e) {
				activityComponentContext.getActivity().getLog()
						.error("Error during web socket data receive", e);
			} finally {
				activityComponentContext.unlockReadRunningRead();
			}
		}

		@Override
		public void sendJson(Object data) {
			try {
				if (activityComponentContext.lockReadRunningRead()) {
					delegate.sendJson(data);
				}
			} catch (Exception e) {
				activityComponentContext.getActivity().getLog()
						.error("Error during web socket JSON sending", e);
			} finally {
				activityComponentContext.unlockReadRunningRead();
			}
		}

		@Override
		public void sendString(String data) {
			try {
				if (activityComponentContext.lockReadRunningRead()) {
					delegate.sendString(data);
				}
			} catch (Exception e) {
				activityComponentContext.getActivity().getLog()
						.error("Error during web socket string sending", e);
			} finally {
				activityComponentContext.unlockReadRunningRead();
			}
		}
	}
}