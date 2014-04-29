package fr.xebia.kouignamann.cloud.mqtt

import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.vertx.groovy.platform.Verticle
import org.vertx.java.core.json.impl.Json

class MqttDataManagementVerticle extends Verticle implements MqttCallback {
    def log

    def client
    MqttConnectOptions options

    def start() {

        log = container.logger

        configure()


        log.info('Start -> Done initialize handler')
    }

    def stop() {
        if(client?.isConnected())
            client?.disconnect()
    }

    def configure() throws MqttException {
        def uri = 'tcp://m10.cloudmqtt.com:10325'
        def clientId = 'cloud-application'

        def persistence = new MemoryPersistence()
        client = new MqttClient(uri, clientId, persistence)

        client.setCallback(this)

        options = new MqttConnectOptions()

        options.setPassword('devoxx'.getChars())
        options.setUserName('devoxxfr')

        options.setCleanSession(false)

        client?.connect(options)
        log.info "MQTT connected"
        client?.subscribe('fr.xebia.kouignamann.nuc.central.processSingleVote', 2)

        // ??
        //client?.disconnect()
    }



    @Override
    void connectionLost(Throwable throwable) {
        log.info "connectionLost", throwable
        // Reconnect
        def i = 0
        while (!client.isConnected()) {
            try {
                client?.connect(options)
                sleep 1000
            } catch (Exception e) {
                log.error "${i} - cannot reconnect", e
            }
            i++
        }
    }

    @Override
    void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
        log.info mqttMessage
        def jsonMessage = Json.decodeValue(new String(mqttMessage.getPayload()), Map)
        def dtInterval = getInterval(new Date(jsonMessage.voteTime))
        vertx.eventBus.send("vertx.database.db",
                [action: "insert", stmt: """
                    INSERT INTO votes VALUES (?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                    `nfc_id` = values(nfc_id),
                    `rasp_id` = values(rasp_id),
                    `slot_dt` = values(slot_dt),
                    `note` = values(note),
                    `dt` = values(dt)
                    """, values: [jsonMessage.nfcId + "_" + dtInterval, jsonMessage.nfcId,
                        jsonMessage.hardwareUid, dtInterval, jsonMessage.note, new Date(jsonMessage.voteTime).format('yyyy-MM-dd HH:mm:ss')]
                ],
                { response ->
                    //logger.info response
                })
    }

    def getInterval(Date date) {
        Calendar c = date.toCalendar()
        if (c.get(Calendar.MINUTE) > 30) {
            c.set(Calendar.HOUR, c.get(Calendar.HOUR) + 1)
        }
        return c.format("YYYY-MM-dd-HH")
    }

    @Override
    void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        //logger.info "deliveryComplete"
    }

}
