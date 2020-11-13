package node.threads

import utils.Connection
import utils.Packet
import utils.PacketType
import java.net.DatagramPacket
import java.net.InetAddress
import java.nio.channels.DatagramChannel
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class Receiver(
    private val datagramChannel: DatagramChannel,
    private val port: Int,
    private val received: ConcurrentLinkedQueue<Packet>,
    private val childs: ConcurrentLinkedQueue<Connection>
) : Runnable {
    //Получаем датаграмм пакет, проверяем есть ли отправитель пакета в детях.
    //Если нет, то добавляем его в childs
    //Печатаем сообщение из пакета
    override fun run() {
        while (true) {
            val datagramPacket = DatagramPacket(ByteArray(1000), 1000)
            datagramChannel.socket().receive(datagramPacket)
            val message = getInfoFromPacket(datagramPacket)
            if (message.getPacketType() != PacketType.HELLO_PACKET) received.add(getInfoFromPacket(datagramPacket))
            val sender = Connection(message.getInetAddress(), message.getPort())
            checkConnectionInChilds(sender)
            println("What i received: ${message.getMessage()}")
        }
    }

    //Проверяем, есть ли отправитель пакета в наших детях, и если нет, то добавляем ему в childs
    fun checkConnectionInChilds(connection: Connection) {
        var haveChild = false
        for (child in childs) {
            if (child.inetAddress == connection.inetAddress && child.port == connection.port) {
                haveChild = true
                break
            }
        }
        if (!haveChild) {
            childs.add(connection)
        }
    }

    //Функция, для извлечения информации о сообщении и его отправителе из датаграмм пакета
    fun getInfoFromPacket(datagramPacket: DatagramPacket): Packet {
        val packet = String(datagramPacket.data, charset("UTF-8"))
        val iter = packet.iterator()
        var packetType = ""
        var uuid = ""
        var ip = ""
        var port = ""
        var mes = ""

        while (iter.hasNext()) {
            val char = iter.nextChar()
            if (char == '\n') break
            packetType += char
        }
        while (iter.hasNext()) {
            val char = iter.nextChar()
            if (char == '\n') break
            uuid += char
        }
        val redunantBackSlash = iter.nextChar()                     //Там в айпишнике лишний слэш в начале
        while (iter.hasNext()) {
            val char = iter.nextChar()
            if (char == '\n') break
            ip += char
        }
        while (iter.hasNext()) {
            val char = iter.nextChar()
            if (char == '\n') break
            port += char
        }
        while (iter.hasNext()) {
            val char = iter.nextChar()
            mes += char
        }
        return Packet(
            Integer.parseInt(packetType).toByte(),
            UUID.fromString(uuid),
            InetAddress.getByName(ip),
            Integer.parseInt(port),
            mes
        )
    }

}