package node

import java.net.InetAddress
import kotlin.system.exitProcess

fun checkPercentage(percent: Int): Boolean {
    return !(percent > 100 || percent < 0)
}

/**   In arguments we have to give name of node, port for binding this node, percentage of loss, [ip node to connect, port of node to connect] **/
fun main(args: Array<String>) {
    when (args.size) {
        3 -> {
            if (checkPercentage(Integer.parseInt(args[2]))) {
                val node = Node(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]))
            } else {
                println("Wrong percentage parameter")
                exitProcess(0)
            }
        }
        5 -> {
            if (checkPercentage(Integer.parseInt(args[2]))) {
                val node = Node(
                    args[0],
                    Integer.parseInt(args[1]),
                    Integer.parseInt(args[2]),
                    InetAddress.getLocalHost(),
                    Integer.parseInt(args[4])
                )
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
    //node.startToSend(1000)
    //node.startToReceive(1001)
}