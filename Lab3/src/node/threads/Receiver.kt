package node.threads

import utils.Packet
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.channels.DatagramChannel
import java.util.concurrent.ConcurrentLinkedQueue

class Receiver(
    private val datagramChannel: DatagramChannel,
    private val port: Int,
    private val received: ConcurrentLinkedQueue<Packet>
) : Runnable {
    //Получаем датаграмм пакет, проверяем есть ли отправитель пакета в детях.
    //Если нет, то добавляем его в childs
    override fun run() {
        while (true) {
            val buffer = ByteArray(1000)
            val datagramPacket = DatagramPacket(buffer, 1000)
            datagramChannel.socket().receive(datagramPacket)
            received.add(getAddressFromPacket(datagramPacket))
            val message = getAddressFromPacket(datagramPacket)
            //val message = String(datagramPacket.data, charset("UTF-8"))
            println("What i received: ${message.getMessage()}")
        }
    }

    //Функция, для извлечения из датаграмм пакета айпи и порта отправителя
    fun getAddressFromPacket(datagramPacket: DatagramPacket) : Packet{
        val packet = String(datagramPacket.data, charset("UTF-8"))
        val iter = packet.iterator()
        var ip = ""
        var port = ""
        var mes = ""
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
        return Packet(InetAddress.getByName(ip), Integer.parseInt(port), mes)
    }

}