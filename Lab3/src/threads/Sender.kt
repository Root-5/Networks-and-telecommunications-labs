package threads

import Node
import message.MessageType
import packet.Packet
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentLinkedQueue

class Sender(private val node: Node, private val packets: ConcurrentLinkedQueue<Packet>) : Runnable {

    private val BUF_SIZE = 1024

    companion object {
        lateinit var ds: DatagramSocket
    }

    init {
        ds = DatagramSocket(node.getPort())
    }

    override fun run() {
        while (true) {
            Thread.sleep(50)
            for (packet in packets)
                if (packet != null) {
                    val byteArrayOutputStream = ByteArrayOutputStream(BUF_SIZE)
                    val address = InetSocketAddress(packet.getSocket().inetAddress, packet.getSocket().port)
                    val objOutputStream = ObjectOutputStream(byteArrayOutputStream)
                    objOutputStream.writeObject(packet.getMessage())
                    val datagramPacket = DatagramPacket(
                        byteArrayOutputStream.toByteArray(),
                        byteArrayOutputStream.toByteArray().size,
                        address
                    )
                    ds.send(datagramPacket)
                    if (packet.getMessage().getMessageType() != MessageType.DEFAULT_MESSAGE) packets.remove(packet)
                }
        }
    }
}