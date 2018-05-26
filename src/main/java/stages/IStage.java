package stages;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Map;

public interface IStage {
    public void proceed(int OP, Selector selector, Map<SocketChannel, IStage> map);
}
