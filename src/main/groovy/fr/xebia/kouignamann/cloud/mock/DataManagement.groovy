package fr.xebia.kouignamann.cloud.mock

import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.platform.Verticle

class DataManagement extends Verticle {
    def logger


    def start() {
        logger = container.logger
        logger.info "Initialize handler";
        [
                "fr.xebia.kouignamann.cloud.data.getNoteRepartition": this.&getNoteRepartition,
                "fr.xebia.kouignamann.cloud.data.getBestSlot": this.&getBestSlot,
        ].each {
            eventBusAddress, handler ->
                vertx.eventBus.registerHandler(eventBusAddress, handler)
        }

        logger.info "Done initialize handler";
    }

    def getNoteRepartition(Message incomingMsg) {
        logger.info("Bus <- fr.xebia.kouignamann.cloud.data.getNoteRepartition ${incomingMsg}")

        vertx.eventBus.send("com.bloidonia.jdbcpersistor",
                [action: "select", stmt: """
                    select slot_id as slot_id, note, count(0) as nbVote from votes
                    inner join rasp_slot on rasp_slot.slot_dt = votes.slot_dt AND rasp_slot.rasp_id = votes.rasp_id
                    where slot_id = ?
                    group by slot_id, note;
                    """
                        , values: [incomingMsg.body.slotId]],
                { response ->
                    logger.info response.body.result
                    def resp = []
                    response.body.result.each{
                        resp << [it.note, it.nbVote]
                    }
                    incomingMsg.reply([
                            "result": resp
                    ])
                })


    }

    def getBestSlot(Message incomingMsg) {
        logger.info("Bus <- fr.xebia.kouignamann.cloud.data.getBestSlot ${incomingMsg}")
        n
        vertx.eventBus.send("com.bloidonia.jdbcpersistor",
                [action: "select", stmt: """
                    select slot_id as slot_id, AVG(note) as avg from votes
                    inner join rasp_slot on rasp_slot.slot_dt = votes.slot_dt AND rasp_slot.rasp_id = votes.rasp_id
                    group by slot_id
                    order by AVG(note) DESC
                    limit 5;
                    """],
                { response ->
                    logger.info response.body.result
                    def resp = []
                    response.body.result.each {
                        // FIXME should be string
                        resp << [slot_id: Integer.parseInt(it.slot_id), avg: it.avg]
                    }
                    incomingMsg.reply([
                            "result": resp
                    ])
                })

    }
}
