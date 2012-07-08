/**
 * 
 */
package org.voyanttools.trombone.util;

import java.io.File;
import java.io.IOException;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * This is a helper method for working with a local web server, especially for testing purposes.
 * @author Stéfan Sinclair, Cyril Briquet
 */
public class EmbeddedWebServer {
	
    private final Server server;
    private static int DEFAULT_PORT = 8889;
    private int port;

    public EmbeddedWebServer() throws IOException {
    	this(DEFAULT_PORT, TestHelper.RESOURCES_PATH);
    }
    public EmbeddedWebServer(int port, String contentsDirectory) throws IOException {

    	if (contentsDirectory == null) {
    		throw new NullPointerException("illegal contents directory");
    	}
    	if (new File(contentsDirectory).exists() == false) {
    		throw new IOException("Embedded web server contents directory doesn't exist: "+contentsDirectory);    		
    	}

    	this.port = port;
    	this.server = new Server();

        final Connector connector = new SelectChannelConnector();
        connector.setPort(port);
        this.server.setConnectors(new Connector[] { connector });

        final WebAppContext webapp = new WebAppContext();
        webapp.setContextPath("/");
        webapp.setWar(contentsDirectory);
        this.server.setHandler(webapp);

	}

    public int getPort() {
    	
    	return this.port;
    	
    }
    
    public synchronized void start() throws Exception {

        this.server.start();

    }

    public synchronized void stop() throws Exception {

        this.server.stop();

    }

}
