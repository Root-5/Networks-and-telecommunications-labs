package node

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class MessageSender(private val socket: DatagramSocket, private val node: Node) : Runnable {
    override fun run() {
        while(node.getNodeStatus() == NodeStatus.isAlive) {
            val buf = "Dima".toByteArray()
            socket.send(DatagramPacket(buf, buf.size, InetAddress.getLocalHost(), 1000))
        }
    }

}