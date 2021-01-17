package emris.snakes.game.event.events;

import emris.snakes.game.event.Event;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import emris.snakes.Network.message.AddressedMessage;

@RequiredArgsConstructor
public class IncomingMessage implements Event {

    public final @NotNull AddressedMessage message;
}
