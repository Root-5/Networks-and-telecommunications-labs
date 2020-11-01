import java.net.*

class App constructor(msg: String, address: String, port: Int) {

    private val timeout = 5000

    private val address: SocketAddress                                      //For multicasting
    private val socket: MulticastSocket = MulticastSocket(port)             //Default port
    private var receiveLastTime: Long
    private var sendLastTime: Long

    private var copies = HashMap<String, Long>()

    init {
        socket.soTimeout = timeout
        this.address = InetSocketAddress(address, port)
        socket.joinGroup(this.address, NetworkInterface.getByInetAddress(InetAddress.getLocalHost()))
        receiveLastTime = 5000
        sendLastTime = 4000
        copies.clear()
        findCopies()
    }

    private fun findCopies() {
        sendLastTime = System.currentTimeMillis()
        while (true) {
            //sending message to users in multicast group with timeout
            if (System.currentTimeMillis() - sendLastTime > timeout) sendMessage("DDOS")
            //receive a message and remember user in map
            val ip = receiveMessage()
            if (ip.isNotEmpty()) copies.put(ip, receiveLastTime)
            //if someone don't send a message for a long time then delete him from map
            for (entry in copies)
                if (System.currentTimeMillis() - entry.value > timeout) copies.remove(entry.key)

            println("Number of live devices: ${copies.size}")
        }
    }

    private fun receiveMessage(): String {
        val requestPacket = DatagramPacket(ByteArray(256), ByteArray(256).size)
        try {
            socket.receive(requestPacket)
        } catch (ex : SocketTimeoutException) {
            return ""
        }
        receiveLastTime = System.currentTimeMillis()
        return requestPacket.address.toString()
    }

    private fun sendMessage(mes: String) {
        socket.send(DatagramPacket(mes.toByteArray(), mes.toByteArray().size, address))
        sendLastTime = System.currentTimeMillis()
    }
}