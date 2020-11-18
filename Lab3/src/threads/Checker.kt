package threads

import connect
import packets
import alternativeIP
import alternativePort
import receivedPackets
import sockets
import time
import java.net.InetAddress
import java.net.Socket

class Checker : Runnable {

    private val TIMEOUT = 5000
    private val MAX_PACKETS = 300

    override fun run() {
        while (true) {
            Thread.sleep(100)
            checkDeadNodes()
            //delete messages
            if(packets.size > MAX_PACKETS){
                var i = 0
                while (i < 100) {
                    i++
                    packets.poll()
                }
            }
            if(receivedPackets.size > MAX_PACKETS){
                var i = 0
                while (i < 100) {
                    i++
                    receivedPackets.poll()
                }
            }
        }
    }

    private fun checkDeadNodes() {
        var i: Int = 0
        var j: Int
        for(t in time.elements()) {
            if(System.currentTimeMillis() - t > TIMEOUT) {
                j = 0
                for (socket in time.keys()) {
                    if(i == j) {
                        reconnect(socket)
                        sockets.remove(socket)
                        time.remove(socket)
                        break
                    }
                    j++
                }
            }
            i++
        }
    }

    private fun reconnect(socket: Socket) {
        if(sockets.peek().inetAddress.toString() == socket.inetAddress.toString() && sockets.peek().port == socket.port) {
            sockets.poll()
            if(alternativeIP != null) {
                println("Connecting to : ${InetAddress.getLocalHost().hostAddress} with port: $alternativePort")                                    //if ip String(parIP.toString().toCharArray(), 1, parIP.toString().length - 1)
                alternativePort?.let { connect(InetAddress.getLocalHost().hostAddress.toString(), it) }        //if ip String(parIP.toString().toCharArray(), 1, parIP.toString().length - 1)
            }
        }
    }
}