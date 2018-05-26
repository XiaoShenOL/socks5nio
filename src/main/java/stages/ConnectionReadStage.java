package stages;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Map;

import static stages.Utils.*;


public class ConnectionReadStage implements IStage {
    public static final int CONST_BUFFER_SIZE = 4;
    public static final int PORT_BUFFER_SIZE = 2;
    public static final int ADDRESS_BUFFER_SIZE = 4;

    public static final int CMD_NUMBER_BYTE_POSITION = 1;
    public static final int ADDRESS_TYPE_BYTE_POSITION = 3;

    public static final byte[] RESPONDING_TEMPLATE = new byte[]{0x05, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,};

    //Error numbers
    public static final byte ERROR_HOST_UNREACHABLE = 0x04;
    public static final byte ERROR_UNSUPPORTED_CMD = 0x07;
    public static final byte ERROR_UNSUPPORTED_ADDRESS_TYPE= 0x08;

    public static final String ADDRESS_SEPARATOR = ".";


    private SocketChannel sourceChannel;
    private SocketChannel distChannel;
    private ByteBuffer buffer;

    public ConnectionReadStage(SocketChannel sourceChannel, ByteBuffer buffer) {
        this.sourceChannel = sourceChannel;
        this.buffer = buffer;
    }

    @Override
    public IStage proceed(int OP, Selector selector, Map<SocketChannel, IStage> stages) {
        try {
//            System.out.println("CON READ STAGE");
            if (!sourceChannel.isOpen()) {
                return null;
            }
            int bytes = sourceChannel.read(buffer);
            buffer.flip();
            if (bytes < 10) {
                buffer.clear();
                return reject(ERROR_SOCKS_SERVER, stages);
            }
            if (buffer.get() != AuthMethodReadStage.SOCKS_VERSION) {
                return reject(ERROR_SOCKS_SERVER, stages);
            }
            if (buffer.get() != CMD_NUMBER) {
                return reject(ERROR_SOCKS_SERVER, stages);
            }
            if (buffer.get() != 0x0) {
                return reject(ERROR_SOCKS_SERVER, stages);
            }
            if (buffer.get() != ADDRESS_TYPE) {
                return reject(ERROR_SOCKS_SERVER, stages);
            }
            byte[] ipv4 = new byte[IPV4_BYTES];
            buffer.get(ipv4);
            short port = buffer.getShort();
            buffer.clear();

            distChannel = SocketChannel.open();
            distChannel.configureBlocking(false);
            distChannel.connect(new InetSocketAddress(InetAddress.getByAddress(ipv4), port));
            distChannel.finishConnect();
            distChannel.register(selector, SelectionKey.OP_CONNECT);
            sourceChannel.register(selector, SelectionKey.OP_READ);
            accept(ipv4, port);
            IStage stage = new ConnectionPendingStage(sourceChannel, distChannel, buffer);
            stages.put(sourceChannel, stage);
            stages.put(distChannel, stage);
            return stage;
        } catch (IOException iOE) {
            iOE.printStackTrace();
        }
        return null;
    }

    private void accept(byte[] ip, short port) {
//        ByteBuffer portBuffer = ByteBuffer.allocate(Short.BYTES).putShort(port);
//        portBuffer.flip();
//        byte[] response = Arrays.copyOf(RESPONDING_TEMPLATE, RESPONDING_TEMPLATE.length);
//        response[1] = ConnectionReadStage.CONNECTION_PROVIDED;
//        response[3] = ip[0];
//        response[4] = ip[1];
//        response[5] = ip[2];
//        response[6] = ip[3];
//        response[7] = portBuffer.get(0);
//        response[8] = portBuffer.get(1);
//        buffer.clear();
//        buffer.put(response);
        buffer.clear();
        buffer.put(SOCKS_VERSION)
                .put(CONNECTION_PROVIDED_CODE)
                .put(RESERVED_BYTE)
                .put(ADDRESS_TYPE)
                .put(ip)
                .putShort(port);
        buffer.flip();
    }

    private IStage reject(byte error, Map<SocketChannel, IStage> map) {
        byte[] response = Arrays.copyOf(RESPONDING_TEMPLATE, RESPONDING_TEMPLATE.length);
        response[1] = error;
        IStage stage = new ConnectionWriteStage(sourceChannel, null, buffer, null, false);
        map.put(sourceChannel, stage);
        return stage;
    }
}
