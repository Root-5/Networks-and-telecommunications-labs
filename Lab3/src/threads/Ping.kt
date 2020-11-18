package threads

import Node
import message.Message
import message.MessageType
import packet.Packet
import java.net.Socket
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class Ping(
    private val sockets: ConcurrentLinkedQueue<Socket>,
    private val packets: ConcurrentLinkedQueue<Packet>,
) : Runnable {
    override fun run() {
        while (true) {
            for (socket in sockets) {
                packets.add(
                    Packet(
                        socket,
                        Message(
                            MessageType.PING_MESSAGE,
                            sockets.peek()!!.inetAddress.toString(),
                            sockets.peek()!!.port.toString(),
                            UUID.randomUUID()
                        )
                    )
                )
                Thread.sleep(100)
            }
        }
    }
}