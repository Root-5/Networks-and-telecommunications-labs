package message

import java.io.Serializable
import java.util.*

class Message(
    private val messageType: MessageType,
    private val name: String,
    private val message: String,
    private val uuid: UUID
) : Serializable {

    /** Getters **/
    fun getUUID() : UUID {
        return uuid
    }

    fun getMessage() : String {
        return message
    }

    fun getName() : String {
        return name
    }

    fun getMessageType() : MessageType {
        return messageType
    }
}