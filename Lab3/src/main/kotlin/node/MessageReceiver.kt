package node

import java.net.DatagramPacket
import java.net.DatagramSocket

class MessageReceiver(private val socket: DatagramSocket, private val node: Node) : Runnable {

    override fun run() {
        socket.soTimeout = 10000
        while (node.getNodeStatus() == NodeStatus.isAlive) {
            val buf = ByteArray(1024)
            socket.receive(DatagramPacket(buf, buf.size))
            println(buf)
        }
    }

}