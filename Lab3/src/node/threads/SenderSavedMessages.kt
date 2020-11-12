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
    private val childs: ConcurrentLinkedQueue<Connection>,
    private val parent: Connection?,
    private val messagesToSend: ConcurrentLinkedQueue<Packet>
) : Runnable {

    //Если есть, что отправть, то проходимся по всем сообщениям, отправляем каждое детям и родителю (Да, я знаю, что надо переделать так, чтобы отправителю пакет назад не доставлялся)
    override fun run() {
        while (true) {
            if (messagesToSend.size != 0) {
                for (packet in messagesToSend) {
                    for (child in childs) {
                        if (child.inetAddress == packet.getInetAddress() && child.port == packet.getPort()) continue
                        val rawMessage = packet.getMessage()
                        val message = addAddressToPacket(rawMessage)
                        val datagramPacket = DatagramPacket(message.toByteArray(Charsets.UTF_8), message.length)
                        datagramPacket.address = child.inetAddress
                        datagramPacket.port = child.port
                        datagramChannel.socket().send(datagramPacket)
                    }
                    if (parent != null) {
                        if (parent.inetAddress == packet.getInetAddress() && parent.port == packet.getPort()) continue
                        val rawMessage = packet.getMessage()
                        val message = addAddressToPacket(rawMessage)
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
    private fun addAddressToPacket(message: String): String {
        return nodeIP.toString() + '\n' + port + '\n' + message
    }

}