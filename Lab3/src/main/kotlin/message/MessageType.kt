package message

class MessageType {
    companion object {
        const val HELLO_MESSAGE: Byte = 0
        const val INFO: Byte = 1
        const val CLIENT_MSG: Byte = 2
        const val CLIENT_MSG_ACK: Byte = 3
    }
}