package utils

import java.net.InetAddress
import java.util.*

class Packet(
        private val packetType: Byte,
        private val uuid: UUID,
        private val inetAddress: InetAddress,
        private val port: Int,
        private val alterNode: Connection,
        private val time: Long,
        private val message: String
) {

    /** Get methods **/

    fun getAlterNode(): Connection {
        return alterNode
    }

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

    fun getTime(): Long {
        return time
    }

    fun getMessage(): String {
        return message
    }
}