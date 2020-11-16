package node.threads

import utils.Connection
import utils.Packet
import utils.PacketType
import java.net.DatagramPacket
import java.net.InetAddress
import java.nio.channels.DatagramChannel
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class SenderTestPackets(
        private val childs: ConcurrentLinkedQueue<Pair<Connection, Connection?>>,
        private val parent: Connection?,
        private val datagramChannel: DatagramChannel,
        private val nodeIP: InetAddress,
        private val port: Int,
        private val alterNode: Connection?
) : Runnable {

    private val TIMEOUT_TO_SEND_TEST_PACKETS: Long = 3000
    override fun run() {
        var startTime = System.currentTimeMillis()
        while (true) {
            if (System.currentTimeMillis() - startTime > TIMEOUT_TO_SEND_TEST_PACKETS) {
                for (child in childs) {
                    val message = addInfoInPacket("Test", PacketType.TEST_PACKET)
                    val packet = DatagramPacket(message.toByteArray(Charsets.UTF_8), message.length)
                    packet.address = child.first.inetAddress
                    packet.port = child.first.port
                    datagramChannel.socket().send(packet)
                }
                if (parent != null) {
                    val message = addInfoInPacket("Test", PacketType.TEST_PACKET)
                    val packet = DatagramPacket(message.toByteArray(Charsets.UTF_8), message.length)
                    packet.address = parent.inetAddress
                    packet.port = parent.port
                    datagramChannel.socket().send(packet)
                }
                startTime = System.currentTimeMillis()
            }
        }
    }

    private fun addInfoInPacket(message: String, packetType: Byte): String {
        //Сначала будет следовать тип сообщения, затем его UUID, после ip и port отправителя, ip и port альтернативной ноды, а далее само сообщение
        val uuid = UUID.randomUUID()
        return if (alterNode == null) {
            "$packetType\n$uuid\n$nodeIP\n$port\n0\n0\n$message"
        } else {
            packetType.toString() + '\n' + uuid.toString() + '\n' + nodeIP.toString() + '\n' + port + '\n' + alterNode.inetAddress + '\n' + alterNode.port + '\n' + System.currentTimeMillis().toString() + '\n' + message
        }
    }

}