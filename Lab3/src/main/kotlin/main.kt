import node.Node
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.*
import kotlin.system.exitProcess


fun main(args: Array<String>) {
    when (args.size) {
        3, 5 -> println("App is loading")
        else -> exitProcess(0)
    }
    val lossPercent = Integer.parseInt(args[2])
    if (lossPercent > 100 || lossPercent < 0) {
        println("Bad loss percent")
        exitProcess(0)
    }
    val port = Integer.parseInt(args[1])
    var parent: InetSocketAddress? = null
    if (args.size == 5) parent = InetSocketAddress(InetAddress.getByName(args[3]), Integer.parseInt(args[4]))
    val node = parent?.let { Node(it, port, lossPercent) }

    val input = Scanner(System.`in`)
    while (true) {
        val message = input.nextLine()
        if (message == "quit") {
            node?.disconnect()
            break
        }
        node?.sendMessage("${args[0]}: $message")
    }
    println("Good bye")
}