package com.gl.homework.client;

import com.gl.homework.utils.Encryption;
import com.gl.homework.utils.MessageUtils;
import lombok.extern.java.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static java.nio.ByteBuffer.allocate;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;

@Log
public class NioClient {
    private static final int NUM_OF_BYTES = 50;
    // The buffer into which we'll read data when it's available
    private ByteBuffer buffer = allocate(NUM_OF_BYTES);
    //port combination to connect to
    private int port;
    // The selector we'll be monitoring
    private Selector selector;
    private InetAddress address;
    //Encryptor to encrypt and decrypt messages
    private Encryption encryptor;
    private SocketChannel socketChannel;
    //Queue for messages to server
    private BlockingQueue<String> blockingQueue;
    //Store message id and encryption key
    private Map<String, String> messageInfoKeyMap = new HashMap<>();

    public static void main(String[] args) throws Exception {
        NioClient nioClient = new NioClient("localhost", 8090);
        nioClient.run();
    }

    private NioClient(String address, int port) throws IOException {
        this.address = InetAddress.getByName(address);
        this.port = port;
    }

    private void run() throws IOException {
        //create socket channel
        socketChannel = SocketChannel.open();
        //non-blocking channel
        socketChannel.configureBlocking(false);
        selector = initSelector();
        //create selector
        socketChannel.register(selector, SelectionKey.OP_CONNECT);
        //new connection event
        socketChannel.connect(new InetSocketAddress(address, port));
        encryptor = new Encryption();


        blockingQueue = new ArrayBlockingQueue<>(10);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Scanner scanner = new Scanner(System.in);
                while (!Thread.interrupted()) {

                    String line = scanner.nextLine();
                    if ("q".equals(line)) {
                        System.exit(0);
                    }
                    try {
                        //generate msg id for current input
                        String currentMessageId = MessageUtils.generateMessageId();
                        //extract receiver user id if it was mentioned
                        int receiverUserId = MessageUtils.extractUserId(line);
                        //delete user id from message
                        if (line.contains("To:")) {
                            line = line.replaceAll("To:([0-9]+)/", "");
                        }

                        MessageInfo messageInfo = new MessageInfo(line, currentMessageId, 0, receiverUserId);
                        if (!line.isEmpty()) {
                            String key = encryptor.generateKey(line.length());
                            String encryptedMessage = encryptor.encrypt(line, key);
                            //add to encrypted msg message info
                            String formatedLine;
                            if (receiverUserId != 0) {
                                formatedLine = String.format("To:%d/msg id:%s/phase:%d/msg:%s", messageInfo.receiverUserId, messageInfo.messageId, messageInfo.phase, encryptedMessage);
                            } else {
                                formatedLine = String.format("msg id:%s/phase:%d/msg:%s", messageInfo.messageId, messageInfo.phase, encryptedMessage);
                            }
                            messageInfoKeyMap.put(currentMessageId, key);
                            blockingQueue.put(formatedLine);
                        }
                    } catch (InterruptedException e) {
                        log.info("Fail to parse user input " + e);
                    }
                    //create new writing event
                    SelectionKey key = socketChannel.keyFor(selector);
                    key.interestOps(OP_WRITE);
                    selector.wakeup();
                }
            }
        }).start();
        waitForEvent();

    }

    private void waitForEvent() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                selector.select(); //wait for events
                //look through all happened events
                for (SelectionKey selectionKey : selector.selectedKeys()) {
                    if (selectionKey.isConnectable()) {
                        //finish connection process
                        socketChannel.finishConnect();
                        System.out.println("I am connected to the server");
                        //get ready to record
                        selectionKey.interestOps(OP_WRITE);
                    } else if (selectionKey.isReadable()) {
                        //read from server
                        read(selectionKey);
                    } else if (selectionKey.isWritable()) {
                        //write to server
                        String line = blockingQueue.poll();
                        if (line != null) {
                            socketChannel.write(ByteBuffer.wrap(line.getBytes()));
                        }
                        selectionKey.interestOps(OP_READ);
                    }
                }
            }
        } catch (IOException | InterruptedException ex) {
            close();
        }
    }

    public void close() {
        try {
            selector.close();

        } catch (IOException e) {
            log.info("Fail to close selector " + e);
        }
    }

    //read from server
    void read(SelectionKey key) throws IOException, InterruptedException {
        SocketChannel channel = (SocketChannel) key.channel();
        buffer.clear();
        StringBuilder sb = new StringBuilder();
        int bytesRead;
        try {
            bytesRead = channel.read(buffer);
        } catch (IOException ex) {
            // The remote forcibly closed the connection, cancel
            // the selection key and close the channel.
            log.info("Reading problem,closing connection" + ex);
            key.cancel();
            channel.close();
            return;
        }
        if (bytesRead == -1) {
            // Remote entity shut the socket down cleanly. Do the
            // same from our end and cancel the channel.
            log.info("Nothing was read from server ");
            key.channel().close();
            key.cancel();
            return;
        }
        while (bytesRead > 0) {
            buffer.flip();
            while (buffer.hasRemaining()) {
                sb.append((char) buffer.get());
            }
            buffer.clear();
            bytesRead = channel.read(buffer);
        }

        buffer.clear();

        String inbox = sb.toString();
        if (!inbox.isEmpty()) {
            parseClientInboxDependOnThePhase(inbox);
        }

        key.interestOps(OP_WRITE);
    }

    private void parseClientInboxDependOnThePhase(String inbox) throws InterruptedException {

        if (inbox.contains("phase")) {
            String phase = MessageUtils.extractPhase(inbox);
            String msgId = MessageUtils.extractMsgId(inbox);
            String sender = MessageUtils.extractSender(inbox);
            String receiver = String.valueOf(MessageUtils.extractUserId(inbox));
            switch (Integer.parseInt(phase)) {
                case 0:
                    String extractMessage = MessageUtils.extractMessage(inbox); //value of encrypted msg
                    if (extractMessage != null) {
                        String keyForDoubleEncryption = encryptor.generateKey(extractMessage.length());
                        if (keyForDoubleEncryption != null && !keyForDoubleEncryption.isEmpty()) {
                            String doubleEncryptedMsg = encryptor.encrypt(extractMessage, keyForDoubleEncryption);
                            messageInfoKeyMap.put(msgId, keyForDoubleEncryption);
                            String lineToSend = String.format("To:%s/:msg id:%s/phase:%d/msg:%s", sender, msgId, 1, doubleEncryptedMsg);
                            blockingQueue.put(lineToSend);
                        }
                    }
                    break;
                case 1:
                    String extractMessage2 = MessageUtils.extractMessage(inbox); //value of encrypted msg
                    if (messageInfoKeyMap.containsKey(msgId)) {
                        String key2 = messageInfoKeyMap.get(msgId);
                        String decryptedMsgOnce = encryptor.decrypt(extractMessage2, key2);
                        String lineToSend = String.format("To:%s/:msg id:%s/phase:%d/msg:%s", sender, msgId, 2, decryptedMsgOnce);
                        blockingQueue.put(lineToSend);
//                        messageInfoKeyMap.remove(msgId);
                    }
                    break;
                case 2:
                    String extractMessage3 = MessageUtils.extractMessage(inbox);
                    if (messageInfoKeyMap.containsKey(msgId)) {
                        String key3 = messageInfoKeyMap.get(msgId);
                        String decryptedMsgTwice = encryptor.decrypt(extractMessage3, key3);
                        String lineToShoe = String.format("From:%s/To:%s/:msg id:%s/phase:%d/msg:%s", sender, receiver, msgId, 3, decryptedMsgTwice);
                        System.out.println(lineToShoe);
                        messageInfoKeyMap.remove(msgId);
                    }
                    break;
                default:
                    log.info("Problem occurred when parsing inbox phase");
            }
        } else {
            System.out.println("Inbox: " + inbox);
        }

    }


    private Selector initSelector() throws IOException {
        // Create a new selector
        return SelectorProvider.provider().openSelector();
    }


}

