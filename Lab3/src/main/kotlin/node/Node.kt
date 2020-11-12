package node

import message.Message
import message.MessageType
import utils.MyHashSet
import utils.MyPair
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.nio.charset.StandardCharsets
import java.util.*

import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.collections.HashMap
import kotlin.collections.HashSet


class Node(private var parent: InetSocketAddress, private val port: Int, private val lossPercent: Int) : Runnable {

    private val queues = LinkedList<ConcurrentLinkedQueue<Message>>()
    private val aliveNeighbors = HashMap<InetSocketAddress, Int>()
    private val acks = MyHashSet<MyPair<UUID, InetSocketAddress>>(100)
    private val messages = MyHashSet<UUID>(100)

    private val messageBuffer = ByteBuffer.wrap(ByteArray(10000))
    private lateinit var datagramChannel: DatagramChannel

    private var myReplacer: InetSocketAddress? = null
    private var parentReplacer: InetSocketAddress? = null

    private val random = Random()
    private var thread: Thread? = null

    private val forDebug: Boolean = true

    init {
        println("dafaq")
        queues.add(ConcurrentLinkedQueue<Message>())
        queues.add(ConcurrentLinkedQueue<Message>())
        queues.add(ConcurrentLinkedQueue<Message>())
        queues.add(ConcurrentLinkedQueue<Message>())
        thread = Thread(this)
        thread!!.start()

    }

    private fun destroyNeighbours() {
        val toErase = HashSet<InetSocketAddress>()
        for (currentNeighbour in aliveNeighbors) {
            if (currentNeighbour.value == 1) toErase.add(currentNeighbour.key)
            else aliveNeighbors[currentNeighbour.key] = currentNeighbour.value - 1
        }

        for (address in toErase) {
            aliveNeighbors.remove(address)
        }

        if (parent != null && !(aliveNeighbors.containsKey(parent))) {
            if (parentReplacer != null) {
                parent = parentReplacer as InetSocketAddress
            }
            regenerateNeighbour(parent)

            if (forDebug) {
                println("Parent switched to $parent")
            }
            parentReplacer = null
        }

        if (myReplacer == null && aliveNeighbors.isNotEmpty() || myReplacer != null && !aliveNeighbors.containsKey(
                myReplacer
            )
        ) {
            myReplacer = if (parent != null) {
                parent
            } else {
                val iterator: Iterator<InetSocketAddress> = aliveNeighbors.keys.iterator()
                if (iterator.hasNext()) {
                    iterator.next()
                } else {
                    null
                }
            }
            if (forDebug) println("My replacer switched to $myReplacer")
        }
    }

    private fun regenerateNeighbour(value: InetSocketAddress) {
        aliveNeighbors.put(value, 2)
    }

    private fun sendHelloMessageParent() {
        messageBuffer.clear()
        messageBuffer.put(MessageType.HELLO_MESSAGE)
        messageBuffer.flip()
        datagramChannel.send(messageBuffer, parent)
    }

    fun disconnect() {
        thread?.interrupt()
        thread?.join()
    }

    fun sendMessage(arg: String) {
        println("dafaq2")
        val uuid = UUID.randomUUID()
        val message = Message(uuid, arg.toByteArray(Charsets.UTF_8), null, System.currentTimeMillis())
        queues.first.add(message)
    }

    override fun run() {
        println("OKESASESEASEA")
        datagramChannel = DatagramChannel.open()
        datagramChannel.bind(InetSocketAddress(port))
        datagramChannel.socket().soTimeout = 1000

        if (parent != null) regenerateNeighbour(parent)

        while (true) {
            if (parent != null) sendHelloMessageParent()
            destroyNeighbours()

            val currentTime = System.currentTimeMillis()
            val timeToExitFromIteration = currentTime + 30000
            for ((i, queue) in queues.withIndex()) {
                while (queue.isNotEmpty()) {
                    if (System.currentTimeMillis() >= timeToExitFromIteration) break

                    val clientMessage = queue.peek() as Message

                    if (clientMessage.enableTime > currentTime) break

                    queue.remove()
                    var sentToAll = true
                    for (currentNeighbour: InetSocketAddress in aliveNeighbors.keys) {
                        if (currentNeighbour == clientMessage.from) continue
                        if (acks.getHashSet().contains(MyPair(clientMessage.uuid, currentNeighbour))) continue
                        sentToAll = false
                        messageBuffer.clear()
                        messageBuffer.put(MessageType.CLIENT_MSG)
                        messageBuffer.putLong(clientMessage.uuid.mostSignificantBits)
                        messageBuffer.putLong(clientMessage.uuid.leastSignificantBits)
                        messageBuffer.putInt(clientMessage.message.size)
                        messageBuffer.flip()
                        datagramChannel.send(messageBuffer, currentNeighbour)
                        if (forDebug) println("Send message to ${clientMessage.uuid}. Attempt #$i")
                    }
                    if (!sentToAll && (i + 1) != 4) {
                        clientMessage.enableTime = System.currentTimeMillis() + 3000
                        queues[i + 1].add(clientMessage)
                    }
                }
            }
            println("AUF")
            while (System.currentTimeMillis() < timeToExitFromIteration) {
                datagramChannel.socket().soTimeout = 100000
                val datagramPacket = DatagramPacket(ByteArray(10000), ByteArray(10000).size)
                datagramChannel.socket().receive(datagramPacket)
                println(datagramPacket.data)
                if (random.nextInt(100) < lossPercent) continue
                val from = datagramPacket.address as InetSocketAddress
                messageBuffer.limit(datagramPacket.length)
                messageBuffer.rewind()
                regenerateNeighbour(from)
                if (messageBuffer.remaining() < 1) continue
                messageBuffer.clear()
                when (messageBuffer.get()) {                                                    //Here we are getting type of message
                    MessageType.HELLO_MESSAGE -> {
                        if (forDebug) println("Got hello from $from")
                        messageBuffer.clear()
                        messageBuffer.put(MessageType.INFO)
                        if (myReplacer != null && myReplacer!! != from) {
                            messageBuffer.putShort(myReplacer!!.port.toShort())
                            val address: ByteArray = myReplacer!!.address.address
                            messageBuffer.put(address.size.toByte())
                            messageBuffer.put(address)
                        } else {
                            messageBuffer.putShort(0)
                            messageBuffer.put(0)
                        }
                        messageBuffer.flip()
                        datagramChannel.send(messageBuffer, from)
                    }
                    MessageType.INFO -> {
                        val port = messageBuffer.short
                        val addressLength: Byte = messageBuffer.get()
                        val address = ByteArray(addressLength.toInt())
                        parentReplacer = if (addressLength.toInt() != 0) {
                            messageBuffer.get(address)
                            InetSocketAddress(InetAddress.getByAddress(address), port.toInt())
                        } else {
                            null
                        }
                        if (forDebug) println("Got replacer node: $parentReplacer from $from")
                    }
                    MessageType.CLIENT_MSG -> {
                        val uuid = UUID(messageBuffer.long, messageBuffer.long)
                        val messageLength = messageBuffer.int
                        val message = ByteArray(messageLength)
                        messageBuffer.get(message)
                        val string = String(message, StandardCharsets.UTF_8)
                        if (!messages.getHashSet().contains(uuid)) {
                            println("-----$string-----")
                            queues.first.add(Message(uuid, message, from, System.currentTimeMillis()))

                            messageBuffer.clear()
                            messageBuffer.put(MessageType.CLIENT_MSG_ACK)
                            messageBuffer.putLong(uuid.mostSignificantBits)
                            messageBuffer.putLong(uuid.leastSignificantBits)
                            messageBuffer.flip()
                            datagramChannel.send(messageBuffer, from)
                            messages.add(uuid)
                        }
                    }
                    MessageType.CLIENT_MSG_ACK -> {
                        val uuid = UUID(messageBuffer.long, messageBuffer.long)
                        if (forDebug) println("Got ach with uuid: $uuid")
                        val pair = MyPair<UUID, InetSocketAddress>(uuid, from)
                        acks.add(pair)
                    }
                }
            }
        }
    }
}