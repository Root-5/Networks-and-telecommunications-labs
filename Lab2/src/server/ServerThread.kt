package server

import java.io.*
import java.net.Socket
import java.nio.charset.StandardCharsets

class ServerThread(private val client: Socket) : Runnable {

    private val TIMEOUT = 3000

    init {
        client.soTimeout = 6000
    }

    private fun checkFileName(fileName: String): Boolean {
        if (fileName.contains("//") || fileName.contains("\\") || fileName.contains(" ") || fileName.contains("/")) {
            return false
        }
        return true
    }

    /**  Return true, if file successfully uploaded, else return false  **/
    private fun upload(file: File, fileSize: Long, socket: Socket): Boolean {

        /**  Get file output and socket input  **/
        val fileOutput = file.outputStream()
        val socketInput = socket.getInputStream()
        val buffer: ByteArray = ByteArray(1024)

        var startTime = System.currentTimeMillis()
        var endTime = startTime
        var iterTime = startTime

        var uploadedSize = 0
        var read = 0                                                            //In past simple)

        while (uploadedSize < fileSize) {
            val readPerIteration = socketInput.readNBytes(buffer, 0, buffer.size)
            if (readPerIteration == 0) break
            read += readPerIteration
            uploadedSize += readPerIteration

            fileOutput.write(buffer, 0, readPerIteration)
            val time = System.currentTimeMillis()
            if (time - iterTime > TIMEOUT) {
                println("For file: " + file.name)
                println("Current speed: " +  read.toFloat() / 1024 / 1024 / (time - iterTime) + "mb/s")
                read = 0
                iterTime = 0
            }
        }
        return true
    }

    private fun checkFileIntegrity(file: File, size: Long, status: Boolean): String {
        return if (file.length() == size) "successful upload"
        else if (!status) "Error in uploading"
        else "Incorrect size"
    }

    override fun run() {
        val socketInputStream = client.getInputStream()
        val clientInput = DataInputStream(socketInputStream)
        val clientOutput = BufferedWriter(OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8))
        val standardDirectory = File("./uploads/")
        standardDirectory.mkdir()

        /**  Read the length and name of file  **/
        var fileLength = clientInput.readInt()
        val fileName: ByteArray = ByteArray(fileLength)
        fileLength = clientInput.readNBytes(fileName, 0, fileLength)
        val realFileName = String(fileName, 0, fileLength, StandardCharsets.UTF_8)

        /**  Check realFileName on incorrect symbols and write to user  **/
        if (!checkFileName(realFileName)) {
            clientOutput.write("Error: incorrect file name")
            clientOutput.flush()
            return
        }

        /**  Read size of file and create file  **/
        val fileSize = clientInput.readLong()
        var file = File("${standardDirectory.absoluteFile}" + File.separator + realFileName)
        if (file.exists()) file = File("${standardDirectory.absoluteFile}" + File.separator + Math.random() + realFileName)
        file.createNewFile()
        val status = upload(file, fileSize, client)
        val answer = checkFileIntegrity(file, fileSize, status)
        clientOutput.write("From server: $fileName - ${answer}\n")
        clientOutput.flush()
        println("Complete")
    }

}