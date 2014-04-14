package fr.xebia.kouignamann.cloud

import fr.xebia.kouignamann.cloud.mock.DataManagement
import fr.xebia.kouignamann.cloud.mock.ScheduleJsonMock
import groovy.json.JsonSlurper
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
        logger.info container.config.get("mqttClient")
        logger.info container.config.get("database.db")
        container.deployWorkerVerticle('groovy:' + DataManagement.class.name, container.config, 1)
        container.deployWorkerVerticle('groovy:' + ScheduleJsonMock.class.name, container.config, 1)
        // container.deployWorkerVerticle('groovy:' + MqttDataManagementVerticle.class.name, container.config.get("mqttClient"), 1)
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

        /**
         * List all track and for each track the best top 3 speaker and slot
         */
        matcher.get('/tracks') { final HttpServerRequest serverRequest ->
            logger.info "HTTP -> ${serverRequest}"

            vertx.eventBus.send("fr.xebia.kouignamann.cloud.mock.getTracks", [:]) { message ->
                logger.info "Process -> fr.xebia.kouignamann.cloud.mock.getTracks replied ${message.body.status}"

                serverRequest.response.putHeader('Content-Type', 'application/json')
                serverRequest.response.putHeader('Access-Control-Allow-Origin', '*')
                serverRequest.response.chunked = true
                serverRequest.response.end(Json.encode([result: message.body.result]))
            }

            /*
                    serverRequest.response.putHeader('Content-Type', 'application/json')
                    serverRequest.response.putHeader('Access-Control-Allow-Origin', '*')
                    serverRequest.response.chunked = true
                    serverRequest.response.end(Json.encode([result: [
                    [track: "Agile",
                            first: [track: "a track with a long name", speaker: "First speaker", notes: [[1, 10], [2, 15], [3, 15], [4, 20], [5, 25]]],
                            second: [track: "a track with a long name", speaker: "First speaker", notes: [[1, 10], [2, 15], [3, 15], [4, 20], [5, 25]]],
                            third: [track: "a track with a long name", speaker: "First speaker", notes: [[1, 10], [2, 15], [3, 15], [4, 20], [5, 25]]]],
                    [track: "Data",
                            first: [track: "a track with a long name", speaker: "First speaker", notes: [[1, 10], [2, 15], [3, 15], [4, 20], [5, 25]]],
                            second: [track: "a track with a long name", speaker: "First speaker", notes: [[1, 10], [2, 15], [3, 15], [4, 20], [5, 25]]],
                            third: [track: "a track with a long name", speaker: "First speaker", notes: [[1, 10], [2, 15], [3, 15], [4, 20], [5, 25]]]],
            ]]))
            */
        }

        /**
         * get all note distribution for this track
         */
        matcher.get('/track/:id') { final HttpServerRequest serverRequest ->
            logger.info "HTTP -> ${serverRequest}"

            vertx.eventBus.send("fr.xebia.kouignamann.cloud.mock.getTrack", [track: serverRequest.params.id]) { message ->
                logger.info "Process -> fr.xebia.kouignamann.cloud.mock.getTrack replied ${message.body.status}"

                serverRequest.response.putHeader('Content-Type', 'application/json')
                serverRequest.response.putHeader('Access-Control-Allow-Origin', '*')
                serverRequest.response.chunked = true
                serverRequest.response.end(Json.encode([result: message.body.result]))

            }

            /*
            serverRequest.response.putHeader('Content-Type', 'application/json')
            serverRequest.response.putHeader('Access-Control-Allow-Origin', '*')
            serverRequest.response.chunked = true

            serverRequest.response.end(Json.encode([track: serverRequest.params.id, result: [
                    [slot: "a track with a long name", speaker: "First speaker", notes: [[1, 10], [2, 15], [3, 15], [4, 20], [5, 25]]],
                    [slot: "a track with a long name", speaker: "Second speaker", notes: [[1, 10], [2, 15], [3, 15], [4, 20], [5, 25]]],
                    [slot: "a track with a long name", speaker: "Third speaker", notes: [[1, 10], [2, 15], [3, 15], [4, 20], [5, 25]]],
                    [slot: "a track with a long name", speaker: "Other speaker", notes: [[1, 10], [2, 15], [3, 15], [4, 20], [5, 25]]],
                    [slot: "a track with a long name", speaker: "Other speaker", notes: [[1, 10], [2, 15], [3, 15], [4, 20], [5, 25]]],
                    [slot: "a track with a long name", speaker: "Other speaker", notes: [[1, 10], [2, 15], [3, 15], [4, 20], [5, 25]]],
                    [slot: "a track with a long name", speaker: "Other speaker", notes: [[1, 10], [2, 15], [3, 15], [4, 20], [5, 25]]],
                    [slot: "a track with a long name", speaker: "Other speaker", notes: [[1, 10], [2, 15], [3, 15], [4, 20], [5, 25]]],
                    [slot: "a track with a long name", speaker: "Other speaker", notes: [[1, 10], [2, 15], [3, 15], [4, 20], [5, 25]]],
                    [slot: "a track with a long name", speaker: "Other speaker", notes: [[1, 10], [2, 15], [3, 15], [4, 20], [5, 25]]],
                    [slot: "a track with a long name", speaker: "Other speaker", notes: [[1, 10], [2, 15], [3, 15], [4, 20], [5, 25]]],
                    [slot: "a track with a long name", speaker: "Other speaker", notes: [[1, 10], [2, 15], [3, 15], [4, 20], [5, 25]]],
                    [slot: "a track with a long name", speaker: "Other speaker", notes: [[1, 10], [2, 15], [3, 15], [4, 20], [5, 25]]]
            ]]))
            */
        }

        matcher.get('/speakers') { final HttpServerRequest serverRequest ->
            logger.info "HTTP -> ${serverRequest}"

            vertx.eventBus.send("fr.xebia.kouignamann.cloud.mock.getSpeakers", [:]) { message ->
                logger.info "Process -> fr.xebia.kouignamann.cloud.mock.getSpeakers replied ${message.body.status}"

                serverRequest.response.putHeader('Content-Type', 'application/json')
                serverRequest.response.putHeader('Access-Control-Allow-Origin', '*')
                serverRequest.response.chunked = true
                serverRequest.response.end(Json.encode([result: message.body.result]))
            }
        }

        matcher.get('/top/slot') { final HttpServerRequest serverRequest ->
            logger.info "HTTP -> ${serverRequest}"

            vertx.eventBus.send("fr.xebia.kouignamann.cloud.data.getBestSlot", [:]) { message ->
                logger.info "Process -> fr.xebia.kouignamann.data.mock.getBestSlot replied ${message.body.result}"

                // For each slot get speaker
                vertx.eventBus.send("fr.xebia.kouignamann.cloud.mock.addSpeakerOfSlot", [slots: message.body.result]) { speakersMsg ->
                    logger.info "Process -> fr.xebia.kouignamann.cloud.mock.addSpeakerOfSlot replied ${speakersMsg.body.status}"
                    serverRequest.response.putHeader('Content-Type', 'application/json')
                    serverRequest.response.putHeader('Access-Control-Allow-Origin', '*')
                    serverRequest.response.chunked = true
                    serverRequest.response.end(Json.encode([result: speakersMsg.body.result]))
                }
            }
        }

        matcher.get('/summary') { final HttpServerRequest serverRequest ->
            logger.info "HTTP -> ${serverRequest}"

            serverRequest.response.putHeader('Content-Type', 'application/json')
            serverRequest.response.putHeader('Access-Control-Allow-Origin', '*')
            serverRequest.response.chunked = true

            serverRequest.response.end(Json.encode([
                    "best-devoxx": [
                            slot: "Approche des architectures microservices avec Vert.x",
                            speaker: "Aurélien Maury",
                            avatarURL: "http://www.gravatar.com/avatar/15706981ae8b52786c1587170bb53da6?s=90px"
                    ],
                    "best-half-day": [
                            slot: "Approche des architectures microservices avec Vert.x",
                            speaker: "Aurélien Maury",
                            avatarURL: "http://www.gravatar.com/avatar/15706981ae8b52786c1587170bb53da6?s=90px"
                    ],
                    "best-language": [
                            slot: "Approche des architectures microservices avec Vert.x",
                            speaker: "Aurélien Maury",
                            avatarURL: "http://www.gravatar.com/avatar/15706981ae8b52786c1587170bb53da6?s=90px"
                    ],
                    "best-future": [
                            slot: "Approche des architectures microservices avec Vert.x",
                            speaker: "Aurélien Maury",
                            avatarURL: "http://www.gravatar.com/avatar/15706981ae8b52786c1587170bb53da6?s=90px"
                    ],
                    "best-java": [
                            slot: "Approche des architectures microservices avec Vert.x",
                            speaker: "Aurélien Maury",
                            avatarURL: "http://www.gravatar.com/avatar/15706981ae8b52786c1587170bb53da6?s=90px"
                    ],
                    "best-agility": [
                            slot: "Approche des architectures microservices avec Vert.x",
                            speaker: "Aurélien Maury",
                            avatarURL: "http://www.gravatar.com/avatar/15706981ae8b52786c1587170bb53da6?s=90px"
                    ],
                    "best-web": [
                            slot: "Approche des architectures microservices avec Vert.x",
                            speaker: "Aurélien Maury",
                            avatarURL: "http://www.gravatar.com/avatar/15706981ae8b52786c1587170bb53da6?s=90px"
                    ],
                    "best-startups": [
                            slot: "Approche des architectures microservices avec Vert.x",
                            speaker: "Aurélien Maury",
                            avatarURL: "http://www.gravatar.com/avatar/15706981ae8b52786c1587170bb53da6?s=90px"
                    ],
                    "best-mobile": [
                            slot: "Approche des architectures microservices avec Vert.x",
                            speaker: "Aurélien Maury",
                            avatarURL: "http://www.gravatar.com/avatar/15706981ae8b52786c1587170bb53da6?s=90px"
                    ],
                    "best-cloud": [
                            slot: "Approche des architectures microservices avec Vert.x",
                            speaker: "Aurélien Maury",
                            avatarURL: "http://www.gravatar.com/avatar/15706981ae8b52786c1587170bb53da6?s=90px"
                    ]
            ]))
        }

        matcher.get('/aggregate/note/:slotid') { final HttpServerRequest serverRequest ->
            logger.info "HTTP -> ${serverRequest}"

            def msg = [slotId: serverRequest.params.get("slotId")]
            logger.info("Bus -> fr.xebia.kouignamann.cloud.data.getNoteRepartition ${msg}")
            vertx.eventBus.send("fr.xebia.kouignamann.cloud.data.getNoteRepartition", msg) { message ->
                logger.info "Process -> fr.xebia.kouignamann.cloud.data.getNoteRepartition replied ${message.body.status}"

                serverRequest.response.putHeader('Content-Type', 'application/json')
                serverRequest.response.putHeader('Access-Control-Allow-Origin', '*')
                serverRequest.response.chunked = true
                serverRequest.response.end(Json.encode([result: message.body.result]))
            }

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

        matcher.get('/devoxxian') { final HttpServerRequest serverRequest ->
            vertx.eventBus.send("vertx.database.db", [action: "select", stmt: "select * from devoxxian;"], { response ->
                def devoxxians = []
                response.body.result.each {
                    devoxxians << [
                            mail: it.mail,
                            name: it.name,
                            twitter: it.twitter,
                            postalCode: it.postalCode,
                            company: it.company,
                            comment: it.comment
                    ]
                }

                serverRequest.response.end(Json.encode(devoxxians))
            })
        }

        matcher.get('/vote/:nfcId') { final HttpServerRequest serverRequest ->
            String nfcId = URLDecoder.decode(serverRequest.params.nfcId, "UTF-8");

            vertx.eventBus.send("vertx.database.db", [action: "select", stmt: "select votes.note as note, rasp_slot.slot_id as talkId" +
                    "from votes " +
                    "inner join rasp_slot on rasp_slot.slot_dt = votes.slot_dt AND rasp_slot.rasp_id = votes.rasp_id " +
                    "where nfc_id = ?;", values: [nfcId]], { response ->
                def votes = []
                response.body.result.each {
                    votes << [
                            note: it.note,
                            talkId: it.talkId
                    ]
                }
                serverRequest.response.end(Json.encode(votes))
            })
        }

        matcher.get('/most-popular') { final HttpServerRequest serverRequest ->
            logger.info "HTTP -> ${serverRequest}"

            vertx.eventBus.send("fr.xebia.kouignamann.cloud.data.getBestSlot", [:]) { message ->
                logger.info "Process -> fr.xebia.kouignamann.data.mock.getBestSlot replied ${message.body.result}"
                serverRequest.response.end(Json.encode(message.body.result))
            }


        }

        return matcher
    }
}
