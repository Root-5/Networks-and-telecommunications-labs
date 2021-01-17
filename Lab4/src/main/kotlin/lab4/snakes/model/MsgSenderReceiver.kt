package lab4.snakes.model

import lab4.snakes.SnakesProto
import java.lang.Thread.sleep
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread


class MsgSenderReceiver(bindPort : Int?, private val mainModel : MainModel) {
    class Pair<T1,T2>(var first : T1, var second : T2) {
        override fun equals(o : Any?) : Boolean {
            val obj = o as Pair<*, *>
            return (first==obj.first) && (second==obj.second)
        }

        override fun hashCode(): Int {
            return first.hashCode()+second.hashCode()*31
        }
    }
    class LimitedSeqIdsStorage(private val limit : Long = 150) {
        val seqIds = HashSet<Pair<InetSocketAddress?, Long>>()

        @Synchronized
        fun addSeqId(addr : InetSocketAddress?, seqId : Long) {
            seqIds.add(Pair(addr, seqId))
            //if (seqIds.size>limit) {
            //    seqIds.remove(seqIds.iterator().next())
            //}
        }

        @Synchronized
        fun containsSeqId(addr : InetSocketAddress?, seqId : Long) : Boolean {
            return seqIds.contains(Pair(addr, seqId))
        }
    }
    val resendTimeout = mainModel.gameConfig.pingDelayMs;
    val maxResendAttempts = mainModel.gameConfig.nodeTimeoutMs/mainModel.gameConfig.pingDelayMs;
    companion object {
        val sleepDelay : Long = 10
        val socketTimeout = 500
    }
    private var curSeq : Long = 1
    class QueueMsg(val to : InetSocketAddress?, val msg : SnakesProto.GameMessage, var resendAttempts : Int, var nextAttemptTime : Long)

    val socket = if (bindPort==null) DatagramSocket() else DatagramSocket(bindPort)

    val msgQueue = ConcurrentLinkedQueue<QueueMsg>()
    val msgResendQueue = ConcurrentLinkedQueue<QueueMsg>()
    val receivedSeqIds = LimitedSeqIdsStorage()
    val ackedSeqIds = LimitedSeqIdsStorage()

    val lastActivity = HashMap<InetSocketAddress,Long>()
    val pingMsg = SnakesProto.GameMessage.newBuilder().setPing(SnakesProto.GameMessage.PingMsg.getDefaultInstance())
    var interrupted = false;
    val sendThread : Thread = thread{
        while (!interrupted || !msgQueue.isEmpty() || !msgResendQueue.isEmpty()) {
            addToQueueResendingMsgs()

            val curTime = System.currentTimeMillis()
            val nei = mainModel.getNeighbours()
            for (it in nei) {
                if (lastActivity.contains(it) && lastActivity[it]!! <= curTime-mainModel.gameConfig.pingDelayMs) {
                    send(it, pingMsg)
                    lastActivity[it] = System.currentTimeMillis()
                }
            }

            while (!interrupted || !msgQueue.isEmpty() || !msgResendQueue.isEmpty()) {
                val msg = msgQueue.poll()
                if (msg==null) {
                    try {
                        sleep(sleepDelay)
                    } catch (e : InterruptedException) {}
                    break
                } else {
                    val receiverAddr = msg.to ?: mainModel.masterAddr
                    val msgBytes = msg.msg.toByteArray()
                    if (receiverAddr!=null && (!ackedSeqIds.containsSeqId(null, msg.msg.msgSeq) || msg.resendAttempts==0)) {
//                        if (msg.msg.hasPing()) {
//                            println("Ping")
//                        }
                        socket.send(DatagramPacket(msgBytes, msgBytes.size, receiverAddr))
                        if (msg.resendAttempts>1) {
                            lastActivity[receiverAddr] = System.currentTimeMillis()
                            msgResendQueue.add(QueueMsg(msg.to, msg.msg,
                                msg.resendAttempts-1, System.currentTimeMillis()+resendTimeout))
                        } else if (msg.resendAttempts!=0) {
                            mainModel.playerLostCallback(msg.to)
                        }
                    }
                }
            }

        }
    }

    private fun addToQueueResendingMsgs() {
        while (true) { //and messages in head of queue are available to resend now
            val msg = msgResendQueue.peek()
            if (msg!=null && msg.nextAttemptTime<=System.currentTimeMillis()) {
                msgQueue.add(msg)
                msgResendQueue.poll()
            } else {
                break;
            }
        }
    }

    val receiveThread = thread{
        val maxUdpSize = 65000
        val buf = ByteArray(maxUdpSize)
        val dp = DatagramPacket(buf, buf.size)
        socket.soTimeout = socketTimeout
        while (!interrupted || !msgQueue.isEmpty() || !msgResendQueue.isEmpty()) {
            try {
                socket.receive(dp)
            } catch (e : SocketTimeoutException) { continue;}

            val prefBuf = buf.copyOf(dp.length)
            val msg = SnakesProto.GameMessage.parseFrom(prefBuf)

            val sender = dp.socketAddress as InetSocketAddress
            if (!msg.hasAck()) {
                val ackMsg = SnakesProto.GameMessage.newBuilder()
                ackMsg.msgSeq = msg.msgSeq
//                println("Msg #"+msg.msgSeq+", sender:"+sender)
                if (!receivedSeqIds.containsSeqId(sender, msg.msgSeq)) {
                    mainModel.msgReceivedCallback(sender, msg, ackMsg)
                    if (msg.msgSeq!=0L) {
                        receivedSeqIds.addSeqId(sender, msg.msgSeq)
                    }
                }
                if (!ackMsg.hasError()) {
                    ackMsg.ack = SnakesProto.GameMessage.AckMsg.newBuilder().build()
                }
                send(sender, ackMsg, false)
            } else {
//                println("Ack #"+msg.msgSeq+", sender:"+sender)
                ackedSeqIds.addSeqId(null, msg.msgSeq)
                mainModel.ackReceivedCallback(sender, msg)
            }
        }
    }

    fun send(to : InetSocketAddress?, msgBuilder : SnakesProto.GameMessage.Builder, isSeq: Boolean = true) {
        if (interrupted) {
            return
        }
        val playerId = mainModel.gameModel.myPlayerId
        if (playerId != null) {
            msgBuilder.senderId = playerId
        }
        if (isSeq) {
            msgBuilder.msgSeq = curSeq++;
        }
        val resendAttempts = if (isSeq) maxResendAttempts else 0
        msgQueue.add(QueueMsg(to, msgBuilder.build(), resendAttempts, System.currentTimeMillis()))

    }

    fun stop() {
        interrupted = true;
        sendThread.interrupt()
        receiveThread.interrupt()
        sendThread.join() //To debug purposes
        receiveThread.join()
    }
}