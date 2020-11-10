package node

import java.net.DatagramSocket
import java.net.InetAddress

class Node constructor(nodeName: String, lossPercent: Int, private val port: Int) {

    /** Default fields for every node in app*/
    private val name = nodeName
    private val percent = lossPercent
    private var ipToConnect: InetAddress? = null
    private var portToConnect: Int? = null
    private var nodeType = NodeType.ROOT
    private val nodeStatus = NodeStatus.isAlive

    private val socket = DatagramSocket(port)


    constructor(name: String, lossPercent: Int, port: Int, ipToConnect: InetAddress, portToConnect: Int) : this(
        name,
        lossPercent,
        port
    ) {
        this.ipToConnect = ipToConnect
        this.portToConnect = portToConnect
        nodeType = NodeType.COMMON
    }

    fun start() {
        println("Node started")

        /** Start the threads, which will receive and send packets**/
        Thread(MessageReceiver(socket, this)).start()
        Thread(MessageSender(socket, this)).start()
    }

    fun getNodeStatus() : NodeStatus {
        return nodeStatus
    }
}