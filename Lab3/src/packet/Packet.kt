package packet

import message.Message
import java.io.Serializable
import java.net.Socket

class Packet(private val socket: Socket, private val message: Message) : Serializable {

    /** Getters **/
    fun getSocket() : Socket {
        return socket
    }

    fun getMessage(): Message {
        return message
    }
}