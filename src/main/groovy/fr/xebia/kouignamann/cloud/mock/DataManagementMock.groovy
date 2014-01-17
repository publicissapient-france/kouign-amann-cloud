package fr.xebia.kouignamann.cloud.mock

import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.platform.Verticle

class DataManagementMock extends Verticle {
    def logger


    def start() {
        logger = container.logger
        logger.info "Initialize handler";
        [
                "fr.xebia.kouignamann.cloud.mock.getNoteRepartition": this.&getNoteRepartition,
        ].each {
            eventBusAddress, handler ->
                vertx.eventBus.registerHandler(eventBusAddress, handler)
        }

        logger.info "Done initialize handler";
    }

    def getNoteRepartition(Message incomingMsg) {
        logger.info("Bus <- fr.xebia.kouignamann.cloud.mock.getNoteRepartition ${incomingMsg}")
        incomingMsg.reply([
                "result": ["1": 5, "2": 8, "3": 20, "4": 35, "5": 12]

        ])
    }
}
