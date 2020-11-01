package server

fun main(args: Array<String>) {
    val port: Int
    if(args.size == 1) port = Integer.parseInt(args[0])
    else if(args.isEmpty()) port = 1488
    else return

    val server = Server(port)
    server.detect()
}