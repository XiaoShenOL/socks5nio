package stages;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Map;

public class ConnectionWriteStage implements IStage {

    private SocketChannel client, server;
    private ByteBuffer clientBuffer, serverBuffer;
    private boolean accepted;

    public ConnectionWriteStage(SocketChannel client, SocketChannel sever, ByteBuffer clientBuffer, ByteBuffer serverBuffer, boolean flag) {
        this.client = client;
        this.server = sever;
        this.clientBuffer = clientBuffer;
        this.serverBuffer = serverBuffer;
        this.accepted = flag;
    }

    @Override
    public IStage proceed(int OP, Selector selector, Map<SocketChannel, IStage> map) {
        try {
//            System.out.println("CON WRITE STAGE");
            while(clientBuffer.hasRemaining()) {
                client.write(clientBuffer);
            }
            if (accepted) {
                IStage clientStage = new CommunicationStage(client, server, clientBuffer, serverBuffer);
                IStage serverStage = new CommunicationStage(server, client, serverBuffer, clientBuffer);
                serverBuffer.flip();
                map.put(client, clientStage);
                map.put(server, serverStage);
                client.register(selector, SelectionKey.OP_READ);
                server.register(selector, SelectionKey.OP_READ);
                return clientStage;
            }
        }catch (IOException iOE) {
            iOE.printStackTrace();
        }
        return null;
    }
}
