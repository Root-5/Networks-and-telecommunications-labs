package utils

import java.net.InetAddress

class Packet(private val inetAddress: InetAddress, private val port: Int, private val message: String) {

    /** Get methods **/
    fun getInetAddress(): InetAddress {
        return inetAddress
    }

    fun getPort(): Int {
        return port
    }

    fun getMessage(): String {
        return message
    }

    fun getPacket(): String {
        return inetAddress.toString() + '\n' + port.toString() + '\n' + message
    }
}