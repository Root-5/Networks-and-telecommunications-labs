package utils

class PacketType {
    companion object {
        public val HELLO_PACKET: Byte = 1
        public val DEFAULT_PACKET: Byte = 2
        public val RECLAIMER_PACKET: Byte = 3
        public val TEST_PACKET: Byte = 4                //Пакет, который будет проверять, работает ли нода-сосед
        //Специальный тип пакета, который будет присылаться при назначени/смене заместителя ноды
    }
}