package node

import java.net.DatagramPacket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.channels.DatagramChannel
import java.util.*

class Node(private val name: String, private val port: Int, private val lossPercentage: Int) {
    private val datagramChannel: DatagramChannel = DatagramChannel.open()
    private var ipToConnect: InetAddress? = null
    private var portToConnect: Int? = null
    private var nodeType = NodeType.ROOT

    constructor(name: String, port: Int, lossPercentage: Int, ipToConnect: InetAddress, portToConnect: Int) : this(
        name,
        port,
        lossPercentage
    ) {
        this.ipToConnect = ipToConnect
        this.portToConnect = portToConnect
        nodeType = NodeType.DEFAULT
        println("Hello from second constructor")
    }

    init {
        println("Hello from init fields")
    }
    /*fun startToSend(port: Int) {
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
        while (true) {
            val buffer = ByteArray(1000)
            datagramChannel.socket().receive(DatagramPacket(buffer, 1000))
            val message = String(buffer, charset("UTF-8"))
            println("What i received: $message")
        }
    }*/
}