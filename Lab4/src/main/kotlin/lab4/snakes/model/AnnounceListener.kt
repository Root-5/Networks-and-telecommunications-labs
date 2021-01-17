package lab4.snakes.model

import lab4.snakes.SnakesProto
import lab4.snakes.controller.MainWindowController
import java.net.*
import java.util.*
import kotlin.collections.HashMap
import kotlin.concurrent.thread

class AnnounceListener(val mwc : MainWindowController) {

    companion object {
        val multicastGroup = InetAddress.getByName("239.192.0.4")
        val multicastPort = 9192
        val maxUdpSize = 65000
        val notAliveTimeout = 2000
        val socketTimeout = 1000
    }

    val alive : HashMap<InetSocketAddress, SnakesProto.GameMessage.AnnouncementMsg> = HashMap()
    val lastAliveTime : HashMap<InetSocketAddress, Long> = HashMap()
    val multicastListeningThread : Thread = thread{
        mwc.refreshListGames(alive)

        val socket = MulticastSocket(multicastPort)
        socket.joinGroup(multicastGroup)
        socket.soTimeout = socketTimeout
        val msgBytes = ByteArray(maxUdpSize)
        val dp = DatagramPacket(msgBytes, 0, msgBytes.size)

        while (!Thread.interrupted()) {
            var shouldUpdateList = false
            try {
                socket.receive(dp)
                val prefixBytes = msgBytes.copyOf(dp.length)
                val msg = SnakesProto.GameMessage.parseFrom(prefixBytes)
                val anMsg = msg.announcement
                val addr = dp.socketAddress as InetSocketAddress
                if (!alive.contains(addr)) {
                    shouldUpdateList = true
                }
                alive[addr] = anMsg
                lastAliveTime[addr] = System.currentTimeMillis()
            } catch (e : SocketTimeoutException) {}
            finally {
                val toErase : ArrayList<InetSocketAddress> = ArrayList()
                val curTime = System.currentTimeMillis()
                for (addr in lastAliveTime) {
                    if (curTime>=addr.value+notAliveTimeout) {
                        toErase.add(addr.key)
                        shouldUpdateList = true
                    }
                }
                for (addr in toErase) {
                    alive.remove(addr)
                    lastAliveTime.remove(addr)
                }

                if (shouldUpdateList) {
                    mwc.refreshListGames(alive)
                }
            }
        }
    }

    fun stop() {
        multicastListeningThread.interrupt()
    }
}