package threads

import Node
import message.Message
import message.MessageType
import packet.Packet
import java.net.Socket
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class Input(
    private val node: Node,
    private val sockets: ConcurrentLinkedQueue<Socket>,
    private val packets: ConcurrentLinkedQueue<Packet>
) : Runnable {
    override fun run() {
        val scanner = Scanner(System.`in`)
        while (true) {
            val line = scanner.nextLine()
            for (socket in sockets) {
                packets.add(
                    Packet(
                        socket,
                        Message(MessageType.DEFAULT_MESSAGE, node.getName(), line, UUID.randomUUID())
                    )
                )
            }
        }
    }
}