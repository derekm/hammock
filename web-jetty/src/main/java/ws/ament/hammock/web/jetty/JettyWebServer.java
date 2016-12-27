/*
 * Copyright 2016 Hammock and its contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ws.ament.hammock.web.jetty;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;
import ws.ament.hammock.web.base.AbstractWebServer;
import ws.ament.hammock.web.api.FilterDescriptor;
import ws.ament.hammock.web.spi.WebServerConfiguration;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.DispatcherType;
import java.util.Arrays;
import java.util.EnumSet;

@ApplicationScoped
public class JettyWebServer extends AbstractWebServer {
    private Server jetty;

    @Inject
    private WebServerConfiguration webServerConfiguration;

    @Override
    public void start() {
        WebAppContext context = new WebAppContext();
        context.setContextPath("/");
        context.setResourceBase(webServerConfiguration.getFileDir());
        context.getObjectFactory().addDecorator(new HammockDecorator());
        super.getInitParams().forEach(context::setInitParameter);
        getListeners().forEach(c -> {
            try {
                context.addEventListener(c.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("Unable to instantiate listener "+c, e);
            }
        });

        getServletContextAttributes().forEach(context::setAttribute);
        context.setAttribute("org.eclipse.jetty.util.DecoratedObjectFactory", new HammockDecorator());
        ServletHolderMapper mapper = new ServletHolderMapper(context.getServletHandler());
        context.getServletHandler().getServletContext();
        getServletDescriptors().forEach(mapper::apply);

        for(FilterDescriptor filterDescriptor : getFilterDescriptors()) {
            DispatcherType[] dispatcherTypesArr = filterDescriptor.dispatcherTypes();
            EnumSet<DispatcherType> dispatcherTypes = EnumSet.copyOf(Arrays.asList(dispatcherTypesArr));
            for(String pattern : filterDescriptor.urlPatterns()) {
                context.addFilter(filterDescriptor.getClazz(), pattern, dispatcherTypes);
            }
        }

        try {
            Server server = new Server();

    		ServerConnector connector = new ServerConnector(server);
    		connector.setPort(webServerConfiguration.getPort());
    		
    		if (webServerConfiguration.isSecuredConfigured()){
	    		HttpConfiguration https = new HttpConfiguration();
	    		https.addCustomizer(new SecureRequestCustomizer());
	    		SslContextFactory sslContextFactory = new SslContextFactory();
	    		sslContextFactory.setKeyStorePath(JettyWebServer.class.getResource(webServerConfiguration.getKeystorePath()).toExternalForm());
	    		sslContextFactory.setKeyStorePassword(webServerConfiguration.getKeystorePassword());
	    		sslContextFactory.setKeyManagerPassword(webServerConfiguration.getKeystorePassword());
	    		
	    		sslContextFactory.setTrustStorePath(JettyWebServer.class.getResource(webServerConfiguration.getTruststorePath()).toExternalForm());
	    		sslContextFactory.setTrustStorePassword(webServerConfiguration.getTruststorePassword());
	    		sslContextFactory.setKeyManagerPassword(webServerConfiguration.getTruststorePassword());
	    		
	    		ServerConnector sslConnector = new ServerConnector(server, new SslConnectionFactory(sslContextFactory, "http/1.1"), new HttpConnectionFactory(https));
	    		sslConnector.setPort(webServerConfiguration.getSecuredPort());
	    		server.setConnectors(new Connector[]{connector, sslConnector});
    		} else{
    			server.setConnectors(new Connector[]{connector});
    		}
            server.setHandler(context);
            server.start();
            jetty = server;
        }
        catch (Exception e) {
            throw new RuntimeException("Unable to start server", e);
        }
    }

    @Override
    public void stop() {
        if(jetty != null) {
            try {
                jetty.stop();
                jetty = null;
            } catch (Exception e) {
                throw new RuntimeException("Unable to stop server", e);
            }
        }
    }
}
