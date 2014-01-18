package fr.xebia.kouignamann.cloud

import fr.xebia.kouignamann.cloud.mock.DataManagementMock
import groovy.json.JsonSlurper
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

        HttpServer server = vertx.createHttpServer()
        server.requestHandler(buildRestRoutes().asClosure())

        final int port = Integer.valueOf(System.getProperty("app.port", "8080"));

        server.listen(port)
        logger.info "Start -> starting HTTP Server. Listening on: ${port}"

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
                serverRequest.response.putHeader('Access-Control-Allow-Origin', '*')
                serverRequest.response.chunked = true
                serverRequest.response.end(Json.encode([result: message.body.result]))
            }

        }

        matcher.get('/') { final HttpServerRequest serverRequest ->
            serverRequest.response.putHeader("Content-Type", "text/html");
            serverRequest.response.end("<html>" +
                    "<head><title>Vertx ClickStart</title></head>" +
                    "<body>" +
                    "<h1>Vertx ClickStart</h1></body>" +
                    "<p>Fork me <a href='https://github.com/CloudBees-community/vertx-gradle-clickstart'>here</a></p>" +
                    "</html>");
        }

        matcher.get('/slot/all') { final HttpServerRequest serverRequest ->
            serverRequest.response.putHeader("Content-Type", "application/json");
            vertx.fileSystem.readFile("schedule.json") { ar ->
                if (ar.succeeded) {
                    def result = []

                    def InputJSON = new JsonSlurper().parseText(ar.result.toString())
                    InputJSON.each {
                        def random = new Random()
                        it.moyenne = random.nextInt(500) / 100
                        result << it


                    }
                    serverRequest.response.end(Json.encode(result))
                } else {
                    logger.error("Failed to read", ar.cause)
                }
            }
        }

        matcher.get('/schedule/all') { final HttpServerRequest serverRequest ->
            serverRequest.response.putHeader("Content-Type", "application/json");
            serverRequest.response.sendFile "schedule.json"
        }

        matcher.get('/slot/:id') { final HttpServerRequest serverRequest ->
            serverRequest.response.putHeader("Content-Type", "application/json");
            vertx.fileSystem.readFile("schedule.json") { ar ->
                if (ar.succeeded) {
                    def InputJSON = new JsonSlurper().parseText(ar.result.toString())
                    InputJSON.each {
                        if (it.id as int == serverRequest.params.id as int) {
                            def notes = [:]
                            def voteCount = 0
                            def voteScore = 0
                            def random = new Random()
                            for (i in 1..5) {
                                def randomInt = random.nextInt(80)
                                notes.put(i, randomInt)
                                voteCount += randomInt
                                voteScore += i * randomInt
                            }
                            it.notes = notes
                            it.moyenne = ((voteScore / voteCount) as double).round(2)
                            serverRequest.response.end(Json.encode(it))
                        }

                    }
                } else {
                    logger.error("Failed to read", ar.cause)
                }
            }


        }

        return matcher
    }
}
