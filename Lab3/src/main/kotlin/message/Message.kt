package message

import java.net.InetSocketAddress
import java.util.*

class Message(
    val uuid: UUID,
    val message: ByteArray,
    val from: InetSocketAddress?,
    var enableTime: Long
) {

}