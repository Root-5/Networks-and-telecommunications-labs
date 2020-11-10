import node.Node
import java.net.InetAddress
import kotlin.system.exitProcess


fun main(args: Array<String>) {
    when (args.size) {
        3, 5 -> println("App is loading")
        else -> exitProcess(0)
    }
    val name = args[0]
    val lossPercent = Integer.parseInt(args[1])
    if (lossPercent > 100 || lossPercent < 0) {
        println("Bad loss percent")
        exitProcess(0)
    }
    val port = Integer.parseInt(args[2])
    when (args.size) {
        3 -> Node(name, lossPercent, port)
        5 -> Node(name, lossPercent, port, InetAddress.getByName(args[3]), Integer.parseInt(args[4]))
    }
}