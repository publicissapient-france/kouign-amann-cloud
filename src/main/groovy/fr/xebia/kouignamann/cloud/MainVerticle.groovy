package fr.xebia.kouignamann.cloud


import org.vertx.groovy.core.http.HttpServer
import org.vertx.groovy.core.http.HttpServerRequest
import org.vertx.groovy.core.http.RouteMatcher
import org.vertx.groovy.platform.Verticle
import org.vertx.java.core.json.impl.Json
import org.vertx.java.core.logging.Logger

class MainVerticle extends Verticle {
    Logger logger

    def start() {
        logger = container.logger
        logger.info "Starting"
        container.deployWorkerVerticle('groovy:' + DataManagementMock.class.name, container.config, 1)

        startHttpServer(container.config.listen, container.config.port)
    }

    private HttpServer startHttpServer(String listeningInterface, Integer listeningPort) {
        logger.info "Start -> starting HTTP Server. Listening on: ${listeningInterface}:${listeningPort}"

        HttpServer server = vertx.createHttpServer()
        server.requestHandler(buildRestRoutes().asClosure())
        server.listen(listeningPort, listeningInterface)
    }

    /**
     * Rest routes building.
     * @return @RouteMatcher
     */
    private RouteMatcher buildRestRoutes() {
        RouteMatcher matcher = new RouteMatcher()

        matcher.get('/aggregate/note') { final HttpServerRequest serverRequest ->
            logger.info "HTTP -> ${serverRequest}"

            def msg = [status: "Next"]
            logger.info("Bus -> fr.xebia.kouignamann.cloud.mock.getNoteRepartition ${msg}")
            vertx.eventBus.send("fr.xebia.kouignamann.cloud.mock.getNoteRepartition", msg) { message ->
                logger.info "Process -> fr.xebia.kouignamann.cloud.mock.getNoteRepartition replied ${message.body.status}"

                serverRequest.response.putHeader('Content-Type', 'application/json')
                serverRequest.response.chunked = true
                serverRequest.response.end(Json.encode([result: message.body.result]))
            }

        }

        return matcher
    }
}
