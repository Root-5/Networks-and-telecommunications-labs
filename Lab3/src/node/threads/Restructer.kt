package node.threads

import utils.Connection
import utils.Packet
import java.lang.NullPointerException
import java.util.concurrent.ConcurrentLinkedQueue

class Restructer(
        private val receivedTestPackets: ConcurrentLinkedQueue<Packet>,
        private val childs: ConcurrentLinkedQueue<Pair<Connection, Connection?>>,
        private var parent: Connection?
) : Runnable {
    private val TIMEOUT: Long = 15000

    override fun run() {
        var start = System.currentTimeMillis()
        while (true) {
            if (System.currentTimeMillis() - start > 5000) {
                checkAliveNodes()
                checkParentAlive()
                //receivedTestPackets.clear()
                start = System.currentTimeMillis()
            }
        }
    }

    private fun checkAliveNodes() {
        for (child in childs) {
            var childIsAlive = false
            for (packet in receivedTestPackets) {
                //Если ребенок присылал нам пакет в течение таймаута, то он живой
                if ((System.currentTimeMillis() - packet.getTime()) < TIMEOUT && packet.getInetAddress() == child.first.inetAddress && packet.getPort() == child.first.port) {
                    childIsAlive = true
                }
            }
            if (!childIsAlive) removeDeadChild(child)
        }
    }

    //Метод для проверки отца
    private fun checkParentAlive() {
        try {
            var isAlive = false
            for (packet in receivedTestPackets) {
                if ((System.currentTimeMillis() - packet.getTime()) < TIMEOUT && packet.getInetAddress() == parent!!.inetAddress && packet.getPort() == parent!!.port) {
                    isAlive = true
                }
            }
            if (!isAlive) parent = null
        } catch (ex: NullPointerException) {
            //println("I have no dad")
        }
    }

    //Убирает не живого ребенка из списка детей и добавляет в список заместителя этого ребенка
    private fun removeDeadChild(deadNode: Pair<Connection, Connection?>) {
        childs.remove(deadNode)
        childs.add(Pair(Connection(deadNode.second!!.inetAddress, deadNode.second!!.port), null))
        Thread.sleep(5000)
    }
}