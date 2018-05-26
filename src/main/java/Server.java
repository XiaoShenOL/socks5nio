import stages.AuthMethodReadStage;
import stages.IStage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static stages.Utils.*;

public class Server {
    private static Properties properties = new Properties();
    private static String serverPortConfigName = "server.port";

    public static void main(String[] args) {
        Map<SocketChannel, IStage> stages = new HashMap<>();

        try (ServerSocketChannel serverChannel = ServerSocketChannel.open();
             Selector selector = Selector.open()){
            properties.load(ClassLoader.getSystemResourceAsStream("configuration.properties"));
            serverChannel.configureBlocking(false);
            serverChannel.bind(new InetSocketAddress(Integer.parseInt(properties.getProperty(serverPortConfigName))));
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            while (true) {
                selector.select();
                Set<SelectionKey> keys = selector.selectedKeys();
                if (keys.isEmpty()) {
                    continue;
                }
                keys.removeIf(key -> {
                    if (!key.isValid()) {
                        return true;
                    }
                    if (key.isAcceptable()) {
                        return accept(stages, key);
                    }

                    if (key.isWritable()) {
                        stages.get(key.channel()).proceed(SelectionKey.OP_WRITE, selector, stages);
                        return true;
                    }

                    if (key.isConnectable()) {
                        stages.get(key.channel()).proceed(SelectionKey.OP_CONNECT, selector, stages);
                        return true;
                    }

                    if (key.isReadable()) {
                        stages.get(key.channel()).proceed(SelectionKey.OP_READ, selector, stages);
                        return true;
                    }
                    return true;
                });
                stages.keySet().removeIf(channel -> !channel.isOpen());
            }

        } catch (IOException e) {
            LogUtils.logException("Unexpected error", e);
        }
    }

    private static boolean accept(Map<SocketChannel, IStage> channelsMap, SelectionKey key) {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel channel = null;
        try {
            System.out.print("Accepting ");
            channel = serverChannel.accept();
            System.out.print(channel.getRemoteAddress());
            channel.configureBlocking(false);
            channel.register(key.selector(), SelectionKey.OP_READ);
            channelsMap.put(channel, new AuthMethodReadStage(channel, ByteBuffer.allocate(BUFFER_SIZE)));
            System.out.println(": accepted");
        } catch (IOException e) {
            System.out.println(": not accepted");
            LogUtils.logException("Failed to process channel " + channel, e);
            if (channel != null) {
                closeChannel(channel);
            }
        }
        return true;
    }

    private static void closeChannel(SocketChannel channel) {
        try {
            System.out.println("Closing channel " + channel.getRemoteAddress());
            channel.close();
        } catch (IOException e) {
            System.err.println("Failed to close channel " + channel);
        }
    }
}
