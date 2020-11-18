package threads

import sockets
import java.net.ServerSocket

class Server(private val serverSocket: ServerSocket) : Runnable {
    override fun run() {
        while(true) {
            serverSocket.accept()
        }
    }
}