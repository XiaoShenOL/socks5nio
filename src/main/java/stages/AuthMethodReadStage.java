package stages;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Map;

public class AuthMethodReadStage implements IStage {
    public static int CONST_BUFFER_SIZE = 2;
    public static int SOCKS_VERSION_BYTE_POSITION = 0;
    public static int METHODS_NUMBER_BYTE_POSITION = 1;
    public static final byte SOCKS_VERSION = 0x05;
    public static final byte AUTH_ACCEPTABLE_METHOD = 0x00;


    private static byte[] ACCEPT_AUTH_METHOD = new byte[]{0x05, 0x00};
    private static byte[] REJECT_AUTH_METHOD = new byte[]{0x05, 0xF};

    private ByteBuffer buffer, offeredMethods;
    private SocketChannel sourceChannel;

    public AuthMethodReadStage(SocketChannel sourceChannel, ByteBuffer buffer) {
        this.buffer = buffer;
        this.sourceChannel = sourceChannel;
    }

    @Override
    public IStage proceed(int OP, Selector selector, Map<SocketChannel, IStage> stages) {
        try {
//            System.out.println("AUTH READ STAGE");
            int bytes = sourceChannel.read(buffer);
            if (bytes < 3) {
                return reject(stages);
            }
            buffer.flip();
            if (buffer.get() != SOCKS_VERSION) {
                return reject(stages);
            }
            int methodsNumber;
            if ((methodsNumber = buffer.get()) < 1) {
                return reject(stages);
            }
            if (!isAcceptableAuthMethod(methodsNumber)) {
                return reject(stages);
            }
            sourceChannel.register(selector, SelectionKey.OP_WRITE);
            return accept(stages);

        } catch (IOException iOE) {
            iOE.printStackTrace();
            return null;
        }
    }

    private IStage accept(Map<SocketChannel, IStage> map) {
        buffer.clear();
        buffer.put(ACCEPT_AUTH_METHOD);
        buffer.flip();
        IStage stage = new AuthMethodWriteStage(sourceChannel, buffer, true);
        map.put(sourceChannel, stage);
        return stage;
    }

    private IStage reject(Map<SocketChannel, IStage> map) {
        buffer.clear();
        buffer.put(REJECT_AUTH_METHOD);
        buffer.flip();
        IStage stage = new AuthMethodWriteStage(sourceChannel, buffer, false);
        map.put(sourceChannel, stage);
        return stage;
    }

    private boolean isAcceptableAuthMethod(int methodsNumber) {
        for(int i = 0; i < methodsNumber; i++) {
            if (buffer.get() == AUTH_ACCEPTABLE_METHOD) {
                return true;
            }
        }
        return false;
    }
}
