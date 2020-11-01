package client

import java.net.InetAddress

fun main(args: Array<String>) {
    if(args.size == 3) {
        val ip = args[0]
        val port = Integer.parseInt(args[1])
        val pathToFile = args[2]
        val client = Client(InetAddress.getByName(ip), port)
        client.send(pathToFile)
    }
    else if(args.isEmpty()) {
        val client = Client(InetAddress.getLocalHost(), 1488)
        client.send("111.exe")
    } else {
        println("Enter normal args, lol")
    }
}