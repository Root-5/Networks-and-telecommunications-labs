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

    //Коллекции для хранения полученных пакетов и подключенных к этому узлу узлов
    private val receivedPackages = ConcurrentLinkedQueue<Packet>()
    private val receivedTestPackets = ConcurrentLinkedQueue<Packet>()
    private val childs = ConcurrentLinkedQueue<Pair<Connection, Connection?>>()
    //private val childsWithAlterNode =
    //    ConcurrentLinkedQueue<Pair<Connection, Connection>>()         //1 - сам ребенок, 2 - его заместитель

    //Тут храним адрес родителя этой ноды
    private var parent: Connection? = null

    //Храним данные о заместителе ноды
    private var alternateNode: Connection? = null

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
        if (nodeType == NodeType.DEFAULT) {             //Если ipToConnect и portToConnect не нулевые, добавим родителя узла
            parent = portToConnect?.let {
                ipToConnect?.let { it1 -> Connection(it1, it) }
            }
            alternateNode = null
        }

        receiver = Thread(Receiver(nodeIP, datagramChannel, port, receivedPackages, childs, alternateNode, receivedTestPackets, parent))
        receiver!!.start()

        sender = Thread(Sender(nodeIP, port, datagramChannel, childs, parent, alternateNode))
        sender!!.start()

        senderReceivedMessages =
                Thread(SenderSavedMessages(nodeIP, port, datagramChannel, childs, parent, receivedPackages, alternateNode))
        senderReceivedMessages!!.start()

    }
}