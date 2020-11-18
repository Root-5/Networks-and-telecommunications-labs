import message.Message
import message.MessageType
import packet.Packet
import threads.*
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.properties.Delegates
import kotlin.system.exitProcess

fun checkPercentage(percent: Int): Boolean {
    return !(percent > 100 || percent < 0)
}

var packets = ConcurrentLinkedQueue<Packet>()
var receivedPackets = ConcurrentLinkedQueue<Message>()
var sockets = ConcurrentLinkedQueue<Socket>()
lateinit var serverSocket: ServerSocket
val time = ConcurrentHashMap<Socket, Long>()
lateinit var node: Node

var alternativeIP: String? = null
var alternativePort: Int? = null

/**   In arguments we have to give name of node, port for binding this node, percentage of loss, [ip node to connect, port of node to connect] **/
fun main(args: Array<String>) {
    when (args.size) {
        3, 5 -> {
            if (checkPercentage(Integer.parseInt(args[1]))) {
                node = Node(args)
            } else {
                println("Wrong percentage parameter")
                exitProcess(0)
            }
        }
        else -> {
            println("Wrong parameters")
            exitProcess(0)
        }
    }

    /** Create server for input **/
    serverSocket = ServerSocket(Integer.parseInt(args[2]))
    Thread(Server(serverSocket)).start()

    /** If this node have a parent, then we will connect **/
    if (args.size == 5) {
        val socket = Socket(
            InetAddress.getLocalHost(),
            Integer.parseInt(args[4])
        )
        sockets.add(socket)
        time[socket] = System.currentTimeMillis()
    }

    /** Creating help threads **/
    Thread(Input(node, sockets, packets)).start()
    Thread(Ping(sockets, packets)).start()
    Thread(Sender(node, packets)).start()
    Thread(Receiver()).start()
    Thread(Checker()).start()
    println("Program started")
    connectToTree(args, "${node.getName()} connected to tree")
}

/** Two version of connect to out tree. First use only from main and need to node with parent **/
fun connectToTree(args: Array<String>, mes: String) {
    if (args.size == 5) packets.add(
        Packet(
            sockets.peek(),
            Message(MessageType.DEFAULT_MESSAGE, "Server", mes, UUID.randomUUID())
        )
    )
}

fun connectToTree(mes: String) {
    packets.add(Packet(sockets.peek(), Message(MessageType.DEFAULT_MESSAGE, "Server", mes, UUID.randomUUID())))
}

fun connect(ip: String, port: Int) {
    val socket = Socket(ip, port)
    sockets.add(socket)
    node.set(socket.localPort)
    time[socket] = System.currentTimeMillis()
    connectToTree("${node.getName()} has been reconnected")
}