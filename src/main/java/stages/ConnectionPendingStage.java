package stages;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Map;

import static stages.ConnectionReadStage.RESPONDING_TEMPLATE;
import static stages.Utils.*;

public class ConnectionPendingStage implements IStage {

    private SocketChannel client, server;
    ByteBuffer clientBuffer;
    public ConnectionPendingStage(SocketChannel client, SocketChannel server, ByteBuffer clientBuffer) {
        this.client = client;
        this.server = server;
        this.clientBuffer = clientBuffer;
    }

    @Override
    public IStage proceed(int operation, Selector selector, Map<SocketChannel, IStage> map) {
        try {
//            System.out.println("CON PENDING STAGE");
            if (!client.isOpen()) {
                if (server.isOpen()) server.close();
                return null;
            }
            if (server.finishConnect()) {
                IStage stage = new ConnectionWriteStage(client, server, clientBuffer, ByteBuffer.allocate(BUFFER_SIZE), true);
                map.put(client, stage);
                map.put(server, stage);
                server.register(selector, SelectionKey.OP_READ);
                client.register(selector, SelectionKey.OP_WRITE);
                return stage;
            } else {
                reject(ERROR_SOCKS_SERVER);
                IStage stage = new ConnectionWriteStage(client, server, clientBuffer, ByteBuffer.allocate(BUFFER_SIZE), false);
                client.register(selector, SelectionKey.OP_WRITE);
                map.put(client, stage);
                if(server.isOpen()) server.close();
                return null;
            }
        } catch (IOException iOE) {
            iOE.printStackTrace();
            return null;
        }
    }


    private void reject(byte error) {
        byte[] response = Arrays.copyOf(RESPONDING_TEMPLATE, RESPONDING_TEMPLATE.length);
        response[1] = error;
        clientBuffer.clear();
        clientBuffer.put(response);
    }
}
