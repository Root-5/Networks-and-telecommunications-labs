import java.net.InetAddress

class Node(args: Array<String>) {
    private val name: String = args[0]
    private val lossPercentage = Integer.parseInt(args[1])
    private var port = Integer.parseInt(args[2])

    /** Getters **/
    fun getName(): String {
        return name
    }

    fun getLossPercentage(): Int {
        return lossPercentage
    }

    fun getPort(): Int {
        return port
    }

    /** Setters **/
    fun set(port: Int) {
        this.port = port
    }


}