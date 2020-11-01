package client

import java.io.*
import java.net.InetAddress
import java.net.Socket
import java.nio.charset.StandardCharsets

class Client(ip: InetAddress, port: Int) {
    private val socket = Socket(ip, port)

    fun send(name: String) {
        val file = File(name)

        /**  initialize inputs and outputs with server and files  **/
        val outputStream = socket.getOutputStream()
        val writerToServer = DataOutputStream(outputStream)
        val readerFromServer = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
        val fileInputStream = FileInputStream(file)

        /**  send name and size of file  **/
        val nameInBytes = file.name.toByteArray(StandardCharsets.UTF_8)
        writerToServer.writeInt(nameInBytes.size)
        writerToServer.write(nameInBytes, 0, nameInBytes.size)
        writerToServer.writeLong(file.length())

        /**  send file to server  **/
        val buffer = ByteArray(1024)
        var read = 0                                            //In past simple
        while (true) {
            read = fileInputStream.read(buffer)
            if(read == -1) break
            writerToServer.write(buffer, 0, read)
        }
        writerToServer.flush()

        /**  close socket and get answer from server  **/
        socket.shutdownOutput()
        val answer = readerFromServer.readLine()
        println(answer)
    }
}