package com.gl.homework.service;

import com.gl.homework.utils.MessageUtils;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import lombok.extern.java.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.gl.homework.service.ChangeRequest.CHANGEOPS;
import static java.nio.ByteBuffer.allocate;
import static java.nio.channels.SelectionKey.*;

@Log
public class NioServer {
    //port  to listen on
    static final int PORT = 8090;
    private static final int NUM_OF_BYTES = 50;
    //for client id generation
    private AtomicInteger userId = new AtomicInteger(1);
    // The channel on which we'll accept connections
    private ServerSocketChannel serverChannel;
    // The selector we'll be monitoring
    private Selector selector;
    // The buffer into which we'll read data when it's available
    private ByteBuffer readBuffer = allocate(NUM_OF_BYTES);
    //map which binds our client id with socket channel and in reverse
    private static final BiMap<Integer, SocketChannel> sockets = HashBiMap.create();

    private final List<ChangeRequest> changeRequests = new LinkedList<>();
    // Maps a SocketChannel to a list of ByteBuffer instances
    //keeps track of the data that will be written to the clients
    private final Map<SocketChannel, List<ByteBuffer>> pendingData = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        new NioServer().run();
    }

    private NioServer() throws IOException {
        System.out.println("Initializing server");
        // Create a new non-blocking server socket channel
        serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        // Bind the server socket to the specified address and port
        InetSocketAddress inetSocketAddress = new InetSocketAddress(PORT);
        serverChannel.socket().bind(inetSocketAddress);
        // Create a new selector
        selector = SelectorProvider.provider().openSelector();
        // Register the server socket channel, indicating an interest in
        // accepting new connections
        serverChannel.register(selector, OP_ACCEPT);
        System.out.println("Server started at port " + PORT + " Waiting new connections...");
        run();
    }


    private void run() {
        try {
            // A run the server as long as the thread is not interrupted.
            while (!Thread.currentThread().isInterrupted()) {
                // Process any pending changes
                processChanges();
                // Wait for an event one of the registered channels
                selector.select();
                // Iterate over the set of keys for which events are available
                Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
                while (selectedKeys.hasNext()) {
                    SelectionKey key = selectedKeys.next();
                    // remove the key so that we don't process this OPERATION again.
                    selectedKeys.remove();
                    // key could be invalid if for example, the client closed the connection.
                    if (!key.isValid()) {
                        continue;
                    }
                    // Check what event is available and deal with it
                    if (key.isAcceptable()) {
                        System.out.println("Accepting connection");
                        accept();
                    } else if (key.isReadable()) {
                        System.out.println("Reading connection");
                        read(key);

                    } else if (key.isWritable()) {
                        System.out.println("Writing...");
                        write(key);
                    }
                }
            }
        } catch (IOException e) {
            log.info("IOException,server of port " + PORT + " terminating.Stack trace:" + e);
        } finally {
            closeConnection();
        }
    }

    //change selector state
    private void processChanges() {
        // Process any  changes
        synchronized (changeRequests) {
            //looking for requests to change selector state
            for (ChangeRequest change : changeRequests) {
                if (change.type == CHANGEOPS) {
                    //change operation selector
                    SelectionKey key = change.socket.keyFor(selector);
                    key.interestOps(change.operationSelector);
                }
            }
            changeRequests.clear();
        }
    }

    // closing our selector and socket.
    void closeConnection() {
        System.out.println("Closing server down");
        if (selector != null) {
            try {
                selector.close();
                serverChannel.socket().close();
                serverChannel.close();
            } catch (IOException e) {
                log.info("Fail to close connection " + e);
            }
        }
    }

    // Add queue the data we want written
    void send(SocketChannel socket, byte[] data) {
        synchronized (changeRequests) {
            // Indicate we want the interest ops set changed
            changeRequests.add(new ChangeRequest(socket, CHANGEOPS, OP_WRITE));
            // And queue the data we want written
            synchronized (pendingData) {
                List<ByteBuffer> queue = pendingData.get(socket);
                if (queue == null) {
                    queue = new ArrayList<>();
                    pendingData.put(socket, queue);
                }
                queue.add(ByteBuffer.wrap(data));
            }
        }
        // Finally, wake up our selecting thread so it can make the required changes
        selector.wakeup();
    }

    private void accept() throws IOException {
        // Accept the connection and make it non-blocking
        SocketChannel socketChannel = serverChannel.accept();
        socketChannel.configureBlocking(false);
        int generatedUserId = generateUserId();
        System.out.format("Connected user %s , userId: %d \r%n ", socketChannel.getRemoteAddress(), generatedUserId);
        // write welcome message
        byte[] welcomeMessage = String.format("Welcome, this is the nio server. Your id is %d \r\nTo chat with the specific user, enter \"To:ID/\"\r\nTo " +
                "disconnect enter \"q\"\n", generatedUserId).getBytes();
        socketChannel.write(ByteBuffer.wrap(welcomeMessage));

        // Register the new SocketChannel with our Selector, indicating
        // we'd like to be notified when there's data waiting to be read
        socketChannel.register(selector, OP_READ);

        sockets.put(generatedUserId, socketChannel);
    }

    //read from console user
    private void read(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        // Clear out our read buffer so it's ready for new data
        readBuffer.clear();

        StringBuilder sb = new StringBuilder();
        int bytesRead;
        try {
            bytesRead = socketChannel.read(readBuffer);
        } catch (IOException ex) {
            // The remote forcibly closed the connection, cancel
            // the selection key and close the channel.
            log.info("Disconnect client id :" + sockets.inverse().get(socketChannel));
            key.cancel();
            socketChannel.close();
            sockets.remove(socketChannel);
            return;
        }
        if (bytesRead == -1) {
            // Remote entity shut the socket down cleanly. Do the
            // same from our end and cancel the channel.
            key.channel().close();
            key.cancel();
            return;
        }
        while (bytesRead > 0) {
            readBuffer.flip();
            while (readBuffer.hasRemaining()) {
                sb.append((char) readBuffer.get());
            }
            readBuffer.clear();
            bytesRead = socketChannel.read(readBuffer);
        }
        readBuffer.clear();
        // Hand the data off to our worker thread
        System.out.println("Reading from " + socketChannel.getRemoteAddress());
        String data = sb.toString();
        System.out.println("Receive: " + data);
        send(socketChannel, data.getBytes());
    }

    //write in user console
    private void write(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        Integer senderId = sockets.inverse().get(socketChannel);

        synchronized (pendingData) {
            List<ByteBuffer> queue = pendingData.get(socketChannel);
            while (!queue.isEmpty()) {
                ByteBuffer buf = queue.get(0);
                buf.rewind();

                String s = new String(buf.array());
                int receiverId = MessageUtils.extractUserId(s);

                //write to specific client
                if (sockets.containsKey(receiverId)) {
                    SocketChannel receiver = sockets.get(receiverId);
                    String serverMsg = String.format("Writing to: %s with id %d", receiver.getRemoteAddress(), receiverId);
                    System.out.println(serverMsg);

                    String clientMsg = String.format("From:%d/%s", senderId, s);
                    ByteBuffer msgInfo = ByteBuffer.wrap(clientMsg.getBytes());
                    receiver.write(msgInfo);

                } else {
                    //write to all clients except sender
                    for (Map.Entry<Integer, SocketChannel> clientInfoEntry : sockets.entrySet()) {
                        int currentReceiverId = clientInfoEntry.getKey();
                        if (currentReceiverId != senderId) {
                            SocketChannel receiverSocketChannel = clientInfoEntry.getValue();
                            //message to client console
                            String messageInfo = String.format("From:%d/To:%d/:%s", senderId, currentReceiverId, s);
                            //message to server console
                            String serverMsg = String.format("Writing to: %s with id %d", receiverSocketChannel.getRemoteAddress(), currentReceiverId);

                            System.out.println(serverMsg);
                            ByteBuffer msg = ByteBuffer.wrap(messageInfo.getBytes());
                            receiverSocketChannel.write(msg);

                        }
                    }
                }
                queue.remove(0);
            }
            if (queue.isEmpty()) {
                // We wrote away all data, so we're no longer interested
                // in writing on this socket. Switch back to waiting for
                // data.
                key.interestOps(OP_READ);
            }
        }
    }

    private int generateUserId() {
        return userId.getAndIncrement();
    }

}




