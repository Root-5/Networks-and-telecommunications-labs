package server

import java.io.File
import java.net.ServerSocket

class Server(private val port: Int) {
    private val serverSocket = ServerSocket(port)

    fun detect() {
        println("Server is working")
        while (true) {
            val socket = serverSocket.accept()
            println("Get connection from user with ip: ${socket.inetAddress}")
            Thread(ServerThread(socket)).start()
        }
    }
}