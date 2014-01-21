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
                "fr.xebia.kouignamann.cloud.mock.getBestSlot": this.&getBestSlot,
        ].each {
            eventBusAddress, handler ->
                vertx.eventBus.registerHandler(eventBusAddress, handler)
        }

        logger.info "Done initialize handler";
    }

    def getNoteRepartition(Message incomingMsg) {
        logger.info("Bus <- fr.xebia.kouignamann.cloud.mock.getNoteRepartition ${incomingMsg}")
        def random = new Random()
        incomingMsg.reply([
                "result": [[1, random.nextInt(10)], [2, random.nextInt(20)], [3, random.nextInt(25)], [4, random.nextInt(20)], [5, random.nextInt(10)]]
        ])
    }

    def getBestSlot(Message incomingMsg) {
        logger.info("Bus <- fr.xebia.kouignamann.cloud.mock.getBestSlot ${incomingMsg}")
        def random = new Random()
        incomingMsg.reply([
                "result": [
                        [slotId: random.nextInt(11) + 1, notes: [[1, random.nextInt(10)], [2, random.nextInt(20)], [3, random.nextInt(25)], [4, random.nextInt(20)], [5, random.nextInt(10)]]],
                        [slotId: random.nextInt(11) + 1, notes: [[1, random.nextInt(10)], [2, random.nextInt(20)], [3, random.nextInt(25)], [4, random.nextInt(20)], [5, random.nextInt(10)]]],
                        [slotId: random.nextInt(11) + 1, notes: [[1, random.nextInt(10)], [2, random.nextInt(20)], [3, random.nextInt(25)], [4, random.nextInt(20)], [5, random.nextInt(10)]]],
                ]])
    }
}
