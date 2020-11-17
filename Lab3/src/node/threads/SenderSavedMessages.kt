package node.threads

import utils.Connection
import utils.Packet
import java.net.DatagramPacket
import java.net.InetAddress
import java.nio.channels.DatagramChannel
import java.util.concurrent.ConcurrentLinkedQueue

class SenderSavedMessages(
        private val nodeIP: InetAddress,
        private val port: Int,
        private val datagramChannel: DatagramChannel,
        private val childs: ConcurrentLinkedQueue<Pair<Connection, Connection?>>,
        private val parent: Connection?,
        private val messagesToSend: ConcurrentLinkedQueue<Packet>,
        private val alterNode: Connection?
) : Runnable {

    //Если есть, что отправить, то проходимся по всем сообщениям, отправляем каждое детям и родителю
    override fun run() {
        while (true) {
            if (messagesToSend.size != 0) {
                for (packet in messagesToSend) {
                    if(childs.isNotEmpty()) {
                        for (child in childs) {
                            if (child.first.inetAddress == packet.getInetAddress() && child.first.port == packet.getPort()) continue
                            val message = addInfoToPacket(packet)
                            val datagramPacket = DatagramPacket(message.toByteArray(Charsets.UTF_8), message.length)
                            datagramPacket.address = child.first.inetAddress
                            datagramPacket.port = child.first.port
                            datagramChannel.socket().send(datagramPacket)
                        }
                    }
                    if (parent != null) {
                        if (parent.inetAddress == packet.getInetAddress() && parent.port == packet.getPort()) continue
                        val message = addInfoToPacket(packet)
                        val datagramPacket = DatagramPacket(message.toByteArray(Charsets.UTF_8), message.length)
                        datagramPacket.address = parent.inetAddress
                        datagramPacket.port = parent.port
                        datagramChannel.socket().send(datagramPacket)
                    }
                    messagesToSend.remove(packet)
                }
            }
        }
    }

    //Функция для вшивания в каждый наш датаграмм пакет адреса отправителя
    private fun addInfoToPacket(packet: Packet): String {
        return packet.getPacketType().toString() + '\n' + packet.getUUID()
                .toString() + '\n' + nodeIP.toString() + '\n' + port + '\n' + packet.getAlterNode().inetAddress.toString() + '\n' + packet.getAlterNode().port.toString() + '\n' + System.currentTimeMillis().toString() + '\n' + packet.getMessage()
    }

}