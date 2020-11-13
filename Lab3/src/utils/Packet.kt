package utils

import java.net.InetAddress
import java.util.*

class Packet(
    private val packetType: Byte,
    private val uuid: UUID,
    private val inetAddress: InetAddress,
    private val port: Int,
    private val message: String
) {

    /** Get methods **/

    fun getPacketType(): Byte {
        return packetType
    }

    fun getUUID(): UUID {
        return uuid
    }

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
        return packetType.toString() + '\n' + uuid.toString() + '\n' + inetAddress.toString() + '\n' + port.toString() + '\n' + message
    }
}