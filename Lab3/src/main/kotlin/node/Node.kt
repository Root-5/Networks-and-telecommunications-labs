package node

import java.net.InetAddress

class Node constructor(nodeName: String, lossPercent: Int, port: Int) {
    private val name = nodeName
    private val percent = lossPercent
    private val port = port
    private var ipToConnect: InetAddress? = null
    private var portToConnect: Int? = null

    constructor(name: String, lossPercent: Int, port: Int, ipToConnect: InetAddress, portToConnect: Int) : this(
        name,
        lossPercent,
        port
    ) {
        this.ipToConnect = ipToConnect
        this.portToConnect = portToConnect
    }
}