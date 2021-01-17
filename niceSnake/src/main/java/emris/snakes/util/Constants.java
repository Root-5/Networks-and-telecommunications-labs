package emris.snakes.util;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;

@UtilityClass
public class Constants {

    public static final int MS_PER_NS = 1_000_000;

    public static final int MULTICAST_PORT = 9192;
    public static final @NotNull String MULTICAST_GROUP = "239.192.0.4";

    public static final @NotNull InetSocketAddress announceAddress
            = new InetSocketAddress(MULTICAST_GROUP, MULTICAST_PORT);

    public static final int ANNOUNCE_DELAY_MS = 1_000;

    public static final int MAX_NAME_LENGTH = 13;

    public static final int MAX_PACKET_SIZE_B = 65_000;
}
