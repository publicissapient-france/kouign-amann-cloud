package fr.xebia.kouignamann.cloud
import fr.xebia.kouignamann.cloud.mock.DataManagement
import fr.xebia.kouignamann.cloud.mock.ScheduleJsonMock
import fr.xebia.kouignamann.cloud.mqtt.MqttDataManagementVerticle
import org.vertx.groovy.core.buffer.Buffer
import org.vertx.groovy.core.eventbus.Message
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
        logger.info container.config.get("database.db")
        container.deployWorkerVerticle('groovy:' + DataManagement.class.name, container.config, 1)
        container.deployWorkerVerticle('groovy:' + ScheduleJsonMock.class.name, container.config, 1)
        container.deployWorkerVerticle('groovy:' + MqttDataManagementVerticle.class.name, container.config.get("mqttClient"), 1)
        //container.deployWorkerVerticle('groovy:' + InsertVoteVerticle.class.name, container.config, 1)
        container.deployModule('com.bloidonia~mod-jdbc-persistor~2.1', container.config.get("database.db"), 1)
        println(container.config.get("database.db"))
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

        matcher.post('/devoxxian') { final HttpServerRequest serverRequest ->
            serverRequest.bodyHandler { Buffer body ->
                def jsonMessage = Json.decodeValue(body.getString(0, body.length), Map)
                vertx.eventBus.send("vertx.database.db",
                        [action: "insert", stmt: """
                    INSERT INTO devoxxian VALUES (?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                    `mail` = values(mail),
                    `name` = values(name),
                    `twitter` = values(twitter),
                    `postalCode` = values(postalCode),
                    `company` = values(company),
                    `comment` = values(comment)
                    """, values: [jsonMessage.mail, jsonMessage.name, jsonMessage.twitter, jsonMessage.postalCode, jsonMessage.company, jsonMessage.comment]
                        ],
                        { Message response ->
                            serverRequest.response.setStatusCode(response.body().status == 'ok' ? 200 : 500)
                            serverRequest.response.end();
                        })
            }
        }

//        matcher.get('/devoxxian') { final HttpServerRequest serverRequest ->
//            vertx.eventBus.send("vertx.database.db", [action: "select", stmt: "select * from devoxxian;"], { response ->
//                def devoxxians = []
//                response.body.result.each {
//                    devoxxians << [
//                            mail: it.mail,
//                            name: it.name,
//                            twitter: it.twitter,
//                            postalCode: it.postalCode,
//                            company: it.company,
//                            comment: it.comment
//                    ]
//                }
//
//                serverRequest.response.end(Json.encode(devoxxians))
//            })
//        }

        matcher.get('/tirageMacBook') { final HttpServerRequest serverRequest ->
            vertx.eventBus.send("vertx.database.db", [action: "select", stmt: "select mail from devoxxian;"], { response ->
                def devoxxians = []
                response.body.result.each {
                    devoxxians << it.mail
                }
                Random rand = new Random()
                serverRequest.response.end(devoxxians[rand.nextInt(devoxxians.size())])
            })
        }

        matcher.get('/vote/:nfcId') { final HttpServerRequest serverRequest ->
            String nfcId = URLDecoder.decode(serverRequest.params.nfcId, "UTF-8");

            vertx.eventBus.send("vertx.database.db", [action: "select", stmt: """select votes.note as note, rasp_slot.slot_id as talkId from votes
inner join rasp_slot on rasp_slot.slot_dt = votes.slot_dt AND rasp_slot.rasp_id = votes.rasp_id
where nfc_id = ?""", values: [nfcId]], { response ->
                def votes = []
                println(response.body.result)
                response.body.result.each {
                    votes << [
                            note: it.note,
                            talkId: it.slot_id
                    ]
                }
                serverRequest.response.end(Json.encode(votes))
            })
        }

        matcher.get('/most-popular') { final HttpServerRequest serverRequest ->
            logger.info "HTTP -> ${serverRequest}"

            vertx.eventBus.send("fr.xebia.kouignamann.cloud.data.getBestSlot", [:]) { message ->
                logger.info "Process -> fr.xebia.kouignamann.data.mock.getBestSlot replied ${message.body.result}"
                serverRequest.response.end(Json.encode(message.body.result.slot_id))
            }
        }

        return matcher
    }
}
