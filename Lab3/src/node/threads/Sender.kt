package node.threads

import utils.Connection
import java.net.DatagramPacket
import java.net.InetAddress
import java.nio.channels.DatagramChannel
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class Sender(
    private val nodeIP: InetAddress,
    private val port: Int,
    private val datagramChannel: DatagramChannel,
    private val childs: ConcurrentLinkedQueue<Connection>,
    private val parent: Connection?
) : Runnable {
    override fun run() {
        //Просто бесконечно читаем из потока ввода, а затем отправляем всем детям и родителю сообщения.
        //Иногда отправляет сообщения, которые пришли от узлов дальше по дереву
        val scanner = Scanner(System.`in`)
        while (true) {
            val rawMessage = scanner.nextLine()
            for (child in childs) {
                val message = addAddressToPacket(rawMessage)
                val datagramPacket = DatagramPacket(message.toByteArray(Charsets.UTF_8), message.length)
                datagramPacket.address = child.inetAddress
                datagramPacket.port = child.port
                datagramChannel.socket().send(datagramPacket)
            }
            if (parent != null) {
                val message = addAddressToPacket(rawMessage)
                val datagramPacket = DatagramPacket(message.toByteArray(Charsets.UTF_8), message.length)
                datagramPacket.address = parent.inetAddress
                datagramPacket.port = parent.port
                datagramChannel.socket().send(datagramPacket)
            }
        }
    }

    //Функция для вшивания в каждый наш датаграмм пакет адреса отправителя
    private fun addAddressToPacket(message: String): String {
        return nodeIP.toString() + '\n' + port + '\n' + message
    }

}