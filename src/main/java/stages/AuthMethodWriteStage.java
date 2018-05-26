package stages;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Map;

public class AuthMethodWriteStage implements IStage {

    private boolean accepted;
    final private SocketChannel channel;
    final private ByteBuffer buffer;

    public AuthMethodWriteStage(SocketChannel sourceChannel, ByteBuffer byteBuffer, boolean flag) {
        this.channel = sourceChannel;
        this.buffer = byteBuffer;
        this.accepted = flag;
    }

    @Override
    public IStage proceed(int OP, Selector selector, Map<SocketChannel, IStage> map) {
        try {
//            System.out.println("AUTH WRITE STAGE");

            if (!channel.isOpen()) {
                buffer.clear();
                return null;
            }
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            buffer.clear();
            if (accepted) {
                channel.register(selector, SelectionKey.OP_READ);
                IStage stage = new ConnectionReadStage(channel, buffer);
                map.put(channel, stage);
                return stage;
            }
        } catch (IOException iOE) {
            iOE.printStackTrace();
        }
        return null;
    }
}
