import java.net.DatagramPacket
import java.net.InetAddress

fun main() {
    println("${InetAddress.getLocalHost().canonicalHostName}")
    println("${InetAddress.getLocalHost().hostAddress}")
    println("${InetAddress.getLocalHost().address}")
}