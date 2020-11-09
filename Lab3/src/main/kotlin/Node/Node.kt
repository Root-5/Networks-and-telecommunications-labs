package Node

import java.net.InetAddress

class Node constructor(nodeName: String, lossPercent: Int, port: Int) {
    private val name = nodeName
    private val percent = lossPercent
    private val port = port


    constructor(name: String, lossPercent: Int, port: Int, ipToConnect: InetAddress, portToConnect: Int) : this(
        name,
        lossPercent,
        port
    ) {

    }
}