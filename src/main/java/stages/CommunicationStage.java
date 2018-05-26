package stages;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Map;

public class CommunicationStage implements IStage{

    private SocketChannel client, server;
    private ByteBuffer clientBuffer, serverBuffer;

    public CommunicationStage(SocketChannel client, SocketChannel server, ByteBuffer clientBuffer, ByteBuffer serverBuffer) throws IOException {
        this.client = client;
        this.server = server;
        this.clientBuffer = clientBuffer;
        this.serverBuffer = serverBuffer;
    }

    @Override
    public IStage proceed(int operation, Selector selector, Map<SocketChannel, IStage> map) {
        try {
            if (!client.isOpen()) {
                if (server.isOpen()) server.close();
                return null;
            }
            if (!server.isOpen()) {
                if (client.isOpen()) client.close();
                return null;
            }
            if (operation == SelectionKey.OP_READ) {
                if (clientBuffer.hasRemaining()) {
                    System.out.println("Has remaining for " + client.getRemoteAddress());
                    return this;
                }
                clientBuffer.clear();
                System.out.print("Reading from " + client.getRemoteAddress() + " ");
                int bytes = client.read(clientBuffer);
                System.out.println(bytes);
                clientBuffer.flip();
                if ( bytes > 0) {
                    server.register(selector, SelectionKey.OP_WRITE);
                } else {
                    client.close();
                    server.close();
                    return null;
                }
            } else if(operation == SelectionKey.OP_WRITE) {
                System.out.print("Writing to " + client.getRemoteAddress() + " ");
                int bytes = 0;
                while(serverBuffer.hasRemaining()) {
                    bytes += client.write(serverBuffer);
                }
                System.out.println(bytes);
                client.register(selector, SelectionKey.OP_READ);
            }
            return this;
        } catch (IOException iOE) {
            iOE.printStackTrace();
            return null;
        }
    }
}
