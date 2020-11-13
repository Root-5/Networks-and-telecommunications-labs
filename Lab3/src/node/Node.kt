package node

import node.threads.Receiver
import node.threads.Sender
import node.threads.SenderSavedMessages
import utils.Connection
import utils.Packet
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.channels.DatagramChannel
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class Node(private val name: String, private val port: Int, private val lossPercentage: Int) {
    private val nodeIP = InetAddress.getByName(InetAddress.getLocalHost().hostAddress)
    private val datagramChannel: DatagramChannel = DatagramChannel.open()
    private var ipToConnect: InetAddress? = null
    private var portToConnect: Int? = null
    private var nodeType = NodeType.ROOT

    //Треды помошники
    private var receiver: Thread? = null
    private var sender: Thread? = null
    private var senderReceivedMessages: Thread? = null
    private val receivedPackages = ConcurrentLinkedQueue<Packet>()
    private val childs = ConcurrentLinkedQueue<Connection>()
    private var parent: Connection? = null

    constructor(name: String, port: Int, lossPercentage: Int, ipToConnect: InetAddress, portToConnect: Int) : this(
        name,
        port,
        lossPercentage
    ) {
        this.ipToConnect = ipToConnect
        this.portToConnect = portToConnect
        nodeType = NodeType.DEFAULT
    }

    fun start() {
        /** set datagram channel settings **/
        datagramChannel.bind(InetSocketAddress(port))
        datagramChannel.socket().soTimeout = 10000000
        if (nodeType == NodeType.DEFAULT) {             //Если ipToConnect и portToConnect не нулевые, выполнится лямбда
            parent = portToConnect?.let {
                ipToConnect?.let { it1 ->
                    Connection(
                        it1,
                        it
                    )
                }
            }
        }

        receiver = Thread(Receiver(datagramChannel, port, receivedPackages, childs))
        receiver!!.start()

        sender = Thread(Sender(nodeIP, port, datagramChannel, childs, parent))
        sender!!.start()

        senderReceivedMessages =
            Thread(SenderSavedMessages(nodeIP, port, datagramChannel, childs, parent, receivedPackages))
        senderReceivedMessages!!.start()

    }
    /*fun startToSend(port: Int) {
        datagramChannel.bind(InetSocketAddress(port))
        datagramChannel.socket().soTimeout = 1000
        val scanner = Scanner(System.`in`)
        datagramChannel.socket().connect(InetAddress.getLocalHost(), 1001)
        while (true) {
            val message = scanner.nextLine()
            datagramChannel.socket().send(DatagramPacket(message.toByteArray(Charsets.UTF_8), message.length))
            println("Message sent")
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