package main;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.ServerContainer;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

public class BattleShipServer {
	public static void main(String[] args) {
		
		int port = 8080;
		try {
			port = Integer.valueOf(System.getenv("PORT"));
		} catch(Exception e) {
			System.out.println("No port specified, defaulting!");
		}
		System.out.println("Starting Server on Port " + port);
		
		Server server = new Server(port);
        ServerConnector connector = new ServerConnector(server);
        connector.setIdleTimeout(1000 * 600);
        //connector.setPort(8080);
        server.addConnector(connector);
        
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        
        try
        {
            // Initialize javax.websocket layer
            ServerContainer wscontainer = WebSocketServerContainerInitializer.configureContext(context);

            // Add WebSocket endpoint to javax.websocket layer
            wscontainer.addEndpoint(BattleShipEndpoint.class);

            server.start();
            //server.dump(System.err);
            server.join();
        }
        catch (Throwable t)
        {
            t.printStackTrace(System.err);
        }
	}
}
