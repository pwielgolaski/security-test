package autheniticationserver;

import com.google.common.base.Splitter;
import org.eclipse.jetty.nosql.mongodb.MongoSessionIdManager;
import org.eclipse.jetty.nosql.mongodb.MongoSessionManager;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.jetty.JettyServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
public class EmbededJettyConfig implements EmbeddedServletContainerCustomizer {


    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    MongoDbFactory mongoDbFactory;

    @Override
    public void customize(final ConfigurableEmbeddedServletContainer container) {
        if (container instanceof JettyEmbeddedServletContainerFactory) {
            ((JettyEmbeddedServletContainerFactory) container)
                    .addServerCustomizers(jettyServerCustomizer());
        }
    }

    @Bean
    public JettyServerCustomizer jettyServerCustomizer() {

        return new JettyServerCustomizer() {

            @Override
            public void customize(final Server server) {
                try {
                    String workerName = InetAddress.getLocalHost().getHostName();
                    workerName = Splitter.on('.').split(workerName).iterator().next();

                    MongoSessionIdManager idMgr = new MongoSessionIdManager(server, mongoDbFactory.getDb().getCollection("sessions"));
                    idMgr.setWorkerName(workerName);
                    server.setSessionIdManager(idMgr);

                    logger.info("Set session id manager for worker {}", workerName);

                    SessionHandler sessionHandler = new SessionHandler();
                    MongoSessionManager mongoMgr = new MongoSessionManager();
                    mongoMgr.setSessionIdManager(server.getSessionIdManager());
                    sessionHandler.setSessionManager(mongoMgr);

                    WebAppContext context = (WebAppContext) server.getHandler();
                    context.setSessionHandler(sessionHandler);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        };
    }
}