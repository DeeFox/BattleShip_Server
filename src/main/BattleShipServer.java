package main;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.ServerContainer;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

public class BattleShipServer {
	public static void main(String[] args) {
		Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(8080);
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
