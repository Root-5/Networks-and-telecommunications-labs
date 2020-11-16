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
    private val ip:InetAddress,
    private val datagramChannel: DatagramChannel,
    private val port: Int,
    private val received: ConcurrentLinkedQueue<Packet>,
    private val childs: ConcurrentLinkedQueue<Pair<Connection, Connection?>>,
    private val alterNode: Connection?,
    private val parent: Connection?
) : Runnable {
    //Получаем датаграмм пакет, проверяем есть ли отправитель пакета в детях.
    //Если нет, то добавляем его в childs
    //Печатаем сообщение из пакета
    override fun run() {
        while (true) {
            val datagramPacket = DatagramPacket(ByteArray(1000), 1000)
            datagramChannel.socket().receive(datagramPacket)
            val message = getInfoFromPacket(datagramPacket)
            if (message.getPacketType() == PacketType.DEFAULT_PACKET) received.add(getInfoFromPacket(datagramPacket))
            val sender = Connection(message.getInetAddress(), message.getPort())
            checkConnectionInChilds(sender, message.getAlterNode())
            println("What i received: ${message.getMessage()}")
        }
    }

    //Проверяем, есть ли отправитель пакета в наших детях, и если нет, то добавляем ему в childs
    fun checkConnectionInChilds(connection: Connection, alterNode: Connection?) {
        var haveChild = false
        for (child in childs) {
            if (child.first.inetAddress == connection.inetAddress && child.first.port == connection.port) {
                haveChild = true
                break
            }
        }
        if(parent != null && parent.inetAddress == connection.inetAddress && parent.port == connection.port) {
            haveChild = true
        }
        if (!haveChild) {
            if (alterNode == null) childs.add(Pair(connection, Connection(null, 0)))
            else childs.add(Pair(connection, alterNode))
        }
    }

    //Функция, для извлечения информации о сообщении и его отправителе из датаграмм пакета
    //TODO: Исправь этот shit код
    fun getInfoFromPacket(datagramPacket: DatagramPacket): Packet {
        val packet = String(datagramPacket.data, charset("UTF-8"))
        val iterator = packet.iterator()
        var alterNodeIp = ""
        var alterNodePort = ""
        var packetType = ""
        var uuid = ""
        var ip = ""
        var port = ""
        var time = ""
        var mes = ""

        while (iterator.hasNext()) {
            val char = iterator.nextChar()
            if (char == '\n') break
            packetType += char
        }
        while (iterator.hasNext()) {
            val char = iterator.nextChar()
            if (char == '\n') break
            uuid += char
        }
        iterator.nextChar()                     //Там в айпишнике лишний слэш в начале
        while (iterator.hasNext()) {
            val char = iterator.nextChar()
            if (char == '\n') break
            ip += char
        }
        while (iterator.hasNext()) {
            val char = iterator.nextChar()
            if (char == '\n') break
            port += char
        }
        while (iterator.hasNext()) {
            val char = iterator.nextChar()
            if (char == '\n') break
            if(char == '/') continue
            alterNodeIp += char
        }
        while (iterator.hasNext()) {
            val char = iterator.nextChar()
            if (char == '\n') break
            alterNodePort += char
        }
        while (iterator.hasNext()) {
            val char = iterator.nextChar()
            mes += char
        }
        val result: Packet
        //Если у нас нет альтернативной ноды, то пихаем нулл
        if (alterNodeIp == "0") {
            result = Packet(
                Integer.parseInt(packetType).toByte(),
                UUID.fromString(uuid),
                InetAddress.getByName(ip),
                Integer.parseInt(port),
                Connection(null, 0),
                mes
            )
        }
        //Если есть, то извлекаем айпи и адрес альернативной ноды
        else {
            result = Packet(
                Integer.parseInt(packetType).toByte(),
                UUID.fromString(uuid),
                InetAddress.getByName(ip),
                Integer.parseInt(port),
                Connection(InetAddress.getByName(alterNodeIp), Integer.parseInt(alterNodePort)),
                mes
            )
        }
        return result
    }

}