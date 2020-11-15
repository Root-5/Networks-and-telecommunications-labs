package node.threads

import utils.Connection
import utils.Packet
import utils.PacketType
import java.net.DatagramPacket
import java.net.InetAddress
import java.nio.channels.DatagramChannel
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class Sender(
    private val nodeIP: InetAddress,
    private val port: Int,
    private val datagramChannel: DatagramChannel,
    private val childs: ConcurrentLinkedQueue<Pair<Connection, Connection?>>,
    private val parent: Connection?,
    private val alterNode: Connection?
) : Runnable {
    override fun run() {
        //Просто бесконечно читаем из потока ввода, а затем отправляем всем детям и родителю сообщения.
        //Иногда отправляет сообщения, которые пришли от узлов дальше по дереву
        if (parent != null) {
            sendHelloMessage()
        }
        val scanner = Scanner(System.`in`)
        while (true) {
            val rawMessage = scanner.nextLine()
            for (child in childs) {
                val message = addInfoInPacket(rawMessage, PacketType.DEFAULT_PACKET)
                val datagramPacket = DatagramPacket(message.toByteArray(Charsets.UTF_8), message.length)
                datagramPacket.address = child.first.inetAddress
                datagramPacket.port = child.first.port
                datagramChannel.socket().send(datagramPacket)
            }
            if (parent != null) {
                val message = addInfoInPacket(rawMessage, PacketType.DEFAULT_PACKET)
                val datagramPacket = DatagramPacket(message.toByteArray(Charsets.UTF_8), message.length)
                datagramPacket.address = parent.inetAddress
                datagramPacket.port = parent.port
                datagramChannel.socket().send(datagramPacket)
            }
        }
    }

    private fun sendHelloMessage() {
        val rawMessage = "Hello from your child!"
        val message = addInfoInPacket(rawMessage, PacketType.HELLO_PACKET)
        val datagramPacket = DatagramPacket(message.toByteArray(Charsets.UTF_8), message.length)
        datagramPacket.address = parent!!.inetAddress
        datagramPacket.port = parent.port
        datagramChannel.socket().send(datagramPacket)
    }

    //Функция для вшивания в каждый наш датаграмм пакет данных: Данные отправителя, тип сообщения, его uuid и само сообщение
    private fun addInfoInPacket(message: String, packetType: Byte): String {
        //Сначала будет следовать тип сообщения, затем его UUID, после ip и port отправителя, ip и port альтернативной ноды, а далее само сообщение
        val uuid = UUID.randomUUID()
        return if (alterNode == null) {
            "$packetType\n$uuid\n$nodeIP\n$port\n0\n0\n$message"
        } else {
            packetType.toString() + '\n' + uuid.toString() + '\n' + nodeIP.toString() + '\n' + port + '\n' + alterNode.inetAddress + '\n' + alterNode.port + '\n' + message
        }

    }

}