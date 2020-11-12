package Node

import java.net.DatagramPacket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.util.*

class Node {
    val datagramChannel: DatagramChannel = DatagramChannel.open()

    fun startToSend(port: Int) {
        datagramChannel.bind(InetSocketAddress(port))
        datagramChannel.socket().soTimeout = 1000
        val scanner = Scanner(System.`in`)
        datagramChannel.socket().connect(InetAddress.getLocalHost(), 1001)
        while (true) {
            val message = scanner.nextLine()
            datagramChannel.socket().send(DatagramPacket(message.toByteArray(Charsets.UTF_8), message.length))
            println("Sended message")
        }
    }

    fun startToReceive(port: Int) {
        datagramChannel.bind(InetSocketAddress(port))
        datagramChannel.socket().soTimeout = 100000
        while(true) {
            val buffer = ByteArray(1000)
            datagramChannel.socket().receive(DatagramPacket(buffer, 1000))
            val message = String(buffer, charset("UTF-8"))
            println("What i received: $message")
        }
    }
}