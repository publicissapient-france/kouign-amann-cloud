package fr.xebia.kouignamann.cloud.mock

import groovy.json.JsonSlurper
import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.platform.Verticle

import java.security.MessageDigest

class ScheduleJsonMock extends Verticle {
    def logger

    def tracks = [:]
    def slots = [:]
    def speakers = [:]

    def start() {
        logger = container.logger

        logger.info "Initialize data";

        vertx.fileSystem.readFile("src/main/resources/schedule-xke.json") { ar ->
            if (ar.succeeded) {
                def inputJSON = new JsonSlurper().parseText(ar.result.toString())
                // Extract map with track and slot id
                inputJSON.slot.each { s ->
                    if (!tracks.containsKey(s.track)) {
                        tracks[s.track] = []
                    }
                    tracks[s.track] << s
                    slots[s.id] = s
                }
            } else {
                logger.error("Failed to read", ar.cause)
            }
        }

        vertx.fileSystem.readFile("src/main/resources/speaker-xke.json") { ar ->
            if (ar.succeeded) {
                def inputJSON = new JsonSlurper().parseText(ar.result.toString())
                // Extract map with track and slot id
                inputJSON.speakers.each { s ->
                    s.gravatar = MessageDigest.getInstance("MD5").
                            digest(s.email.getBytes("UTF-8")).
                            encodeHex().
                            toString()
                    speakers[s.id] = s
                }

            } else {
                logger.error("Failed to read", ar.cause)
            }
        }

        logger.info "Initialize handler";
        [
                "fr.xebia.kouignamann.cloud.mock.getTracks": this.&getTracks,
                "fr.xebia.kouignamann.cloud.mock.getTrack": this.&getTrack,
                "fr.xebia.kouignamann.cloud.mock.getSpeakers": this.&getSpeakers,
                "fr.xebia.kouignamann.cloud.mock.addSpeakerOfSlot": this.&addSpeakerOfSlot,
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

    def getSpeakers(Message incomingMsg) {
        logger.info("Bus <- fr.xebia.kouignamann.cloud.mock.getSpeakers ${incomingMsg}")
        incomingMsg.reply([
                "result": speakers

        ])
    }

    def addSpeakerOfSlot(Message incomingMsg) {
        logger.info("Bus <- fr.xebia.kouignamann.cloud.mock.addSpeakerOfSlot ${incomingMsg}")
        def reply = []
        incomingMsg.body.slots.each { it ->
            def slot = slots[it.slotId]
            def speaker = speakers[slot.speakerId]
            it.slot = slot
            it.speaker = speaker
            reply << it
        }
        incomingMsg.reply([
                "result": reply

        ])
    }

    def getTrack(Message incomingMsg) {
        logger.info("Bus <- fr.xebia.kouignamann.cloud.mock.getTrack ${incomingMsg}")
        incomingMsg.reply([
                "result": tracks[incomingMsg.body.track]
        ])
    }
}
