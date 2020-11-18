package threads

import message.Message
import message.MessageType
import node
import packet.Packet
import packets
import alternativeIP
import alternativePort
import receivedPackets
import sockets
import time
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import java.net.DatagramPacket
import java.net.Socket
import java.util.concurrent.ThreadLocalRandom

class Receiver : Runnable {

    private val BUF_SIZE = 1024

    override fun run() {
        while (true) {
            val receivedPacket = DatagramPacket(ByteArray(BUF_SIZE), BUF_SIZE)
            Sender.ds.receive(receivedPacket)
            val objectInputStream = ObjectInputStream(ByteArrayInputStream(receivedPacket.data))
            val message = objectInputStream.readObject() as Message
            when (message.getMessageType()) {
                MessageType.DEFAULT_MESSAGE -> {
                    if (node.getLossPercentage() > ThreadLocalRandom.current().nextInt(0, 100)) break
                    else {
                        var haveThisMessage = false
                        for (mes in receivedPackets)
                            if (mes.getUUID() == message.getUUID()) haveThisMessage = true
                        if (haveThisMessage) sendVerify(message, receivedPacket)
                        else addNewPacket(message, receivedPacket)
                    }
                }
                MessageType.VERIFY_MESSAGE -> {
                    for (packet in packets) if (packet.getMessage().getUUID() == message.getUUID()) {
                        packets.remove(packet)
                        break
                    }
                }
                MessageType.PING_MESSAGE -> {
                    var haveSocket = false
                    for (socket in sockets) if (socket.port == receivedPacket.port) haveSocket = true
                    if (!haveSocket) {
                        val socket = Socket(
                            String(
                                receivedPacket.address.toString().toCharArray(),
                                1,                                                                      //Have a redundant slash "/" in ip
                                receivedPacket.address.toString().length - 1
                            ), receivedPacket.port
                        )
                        sockets.add(socket)
                        time[socket] = System.currentTimeMillis()
                    }
                    //Update times in sockets
                    for (socket in time.keys()) if (socket.port == receivedPacket.port) time.replace(
                        socket,
                        System.currentTimeMillis()
                    )
                    //Get the next
                    if (message.getName() == "/" + receivedPacket.address.hostAddress && message.getMessage()
                            .toInt() == node.getPort()
                    ) {
                        alternativeIP = null
                        alternativePort = null
                    } else {
                        alternativeIP = message.getName()
                        alternativePort = Integer.parseInt(message.getMessage())
                    }
                }
            }
        }
    }

    private fun addNewPacket(message: Message, packet: DatagramPacket) {
        for (socket in sockets) {
            if (socket.port == packet.port) packets.add(
                Packet(
                    socket,
                    Message(MessageType.VERIFY_MESSAGE, node.getName(), " Successful", message.getUUID())
                )
            )
            else packets.add(
                Packet(
                    socket,
                    Message(MessageType.DEFAULT_MESSAGE, node.getName(), message.getMessage(), message.getUUID())
                )
            )
        }
        receivedPackets.add(message)
        println("From ${message.getName()}: ${message.getMessage()}")
    }

    private fun sendVerify(message: Message, packet: DatagramPacket) {
        val mes = Message(MessageType.VERIFY_MESSAGE, node.getName(), " Successful", message.getUUID())
        for (socket in sockets) {
            if (socket.port == packet.port) {
                packets.add(Packet(socket, mes))
                break
            }
        }
    }
}