package stages;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Map;

public interface IStage {
    public IStage proceed(int OP, Selector selector, Map<SocketChannel, IStage> map);
}
