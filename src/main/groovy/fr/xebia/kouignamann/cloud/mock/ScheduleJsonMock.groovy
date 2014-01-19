package fr.xebia.kouignamann.cloud.mock
import groovy.json.JsonSlurper
import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.platform.Verticle

class ScheduleJsonMock extends Verticle {
    def logger

    def tracks = [:]
    def slots = [:]

    def start() {
        logger = container.logger

        logger.info "Initialize data";

        vertx.fileSystem.readFile("schedule-xke.json") { ar ->
            if (ar.succeeded) {
                def inputJSON = new JsonSlurper().parseText(ar.result.toString())
                // Extract map with track and slot id
                inputJSON.slot.each{ s ->
                    if(!tracks.containsKey(s.track)){
                        tracks[s.track] = []
                    }
                    tracks[s.track] << s
                    slots[s.id] = s
                }
            } else {
                logger.error("Failed to read", ar.cause)
            }
        }

        logger.info "Initialize handler";
        [
                "fr.xebia.kouignamann.cloud.mock.getTracks": this.&getTracks,
                "fr.xebia.kouignamann.cloud.mock.getTrack": this.&getTrack,
        ].each {
            eventBusAddress, handler ->
                vertx.eventBus.registerHandler(eventBusAddress, handler)
        }

        logger.info "Done initialize handler";
    }

    def getTracks(Message incomingMsg) {
        logger.info("Bus <- fr.xebia.kouignamann.cloud.mock.getTracks ${incomingMsg}")
        incomingMsg.reply([
                "result": tracks

        ])
    }

    def getTrack(Message incomingMsg) {
        logger.info("Bus <- fr.xebia.kouignamann.cloud.mock.getTrack ${incomingMsg}")
        incomingMsg.reply([
                "result": tracks[incomingMsg.body.track]

        ])
    }
}
