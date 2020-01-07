package com.company;

import java.io.IOException;
import java.util.Random;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;


abstract class IFunction {
    abstract int func(int x) throws InterruptedException, IOException;
    int returnNonZeroValue() {
        return new Random().nextInt(100) + 1;
    }
    int returnZeroValue() {
        return 0;
    }
    int hangs() throws InterruptedException {
        while ( true ) {
            System.out.println("[IFunction::hangs()] I'm hangs :)");
            Thread.sleep(2500);
        }
    }

    enum Type {
        HANG,
        NORMAL
    };

    String m_name;
    Type m_fType;
}

class Function1 extends IFunction {
    @Override
    int func(int x) throws InterruptedException, IOException {
        switch (x) {
            case 1:
                System.out.println("[Function1::f()] Going to send non zero value");
                return returnNonZeroValue();
            case 2:
                System.out.println("[Function1::f()] Going to send zero value");
                return returnZeroValue();
            case 3:
                System.out.println("[Function1::f()] Going to hangs");
                return hangs();
            case 4:
                System.out.println("[Function1::f()] Going to send non zero value slowly");
                Thread.sleep(100);
                return returnNonZeroValue();
            case 5:
                System.out.println("[Function1::f()] Going to send zero value slowly");
                Thread.sleep(100);
                return returnZeroValue();
            default:
                System.out.println("[Function1::f()] Incorrect x value: " + x);
        }
        return 0;
    }
}

class Client extends Thread {
    Client(IFunction func) {
        m_func = func;
    }

    public void ConnectToServer(String ipAddr, int port) throws IOException {
        m_serverAddress = new InetSocketAddress(ipAddr, port);
        m_channel = SocketChannel.open(m_serverAddress);
    }
    void ResponseToServer(Integer x) throws IOException {
        System.out.println("[Client::ResponseToServer()] Going to sent message back to the server...");
        byte[] message = new String(x.toString()).getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.wrap(message);
        m_channel.write(buffer);
    }
    public void GetInfoFromServer() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        System.out.println("[Client::GetInfoFromServer(" + Thread.currentThread().getId()
                + ")] Waiting for a server arguments...");

        int numRead = 0;
        do {
            numRead = m_channel.read(buffer);
        } while (numRead == 0);

        parseArguments(buffer, numRead);
    }
    public int RunFunction() throws IOException, InterruptedException {
        return m_func.func(m_x);
    }

    @Override
    public void run() {
        try {
            GetInfoFromServer();
            int returnValue = RunFunction();
            System.out.println("[ClientL::run()] Before send message to server, message: " + returnValue);
            sendReturnValueToServer(returnValue);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
    private void parseArguments(ByteBuffer buf, int size) {
        System.out.println("[Client::parseArguments()] Parsing received param...");

        buf.flip();
        byte[] data = new byte[size];
        System.arraycopy(buf.array(), 0, data, 0, size);
        String[] splitedMessages = new String(data).split(" ");

        m_x = Integer.parseInt(splitedMessages[0]);

        System.out.println("[Client::parseArguments(" + Thread.currentThread().getId() + ")] received x: " + m_x);
    }
    private void sendReturnValueToServer(Integer rv) throws IOException {
        byte[] message = new String(rv.toString()).getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.wrap(message);
        m_channel.write(buffer);
    }

    private SocketChannel m_channel;
    private InetSocketAddress m_serverAddress;
    private IFunction m_func;
    private Integer m_x;
};

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        Function1 f = new Function1();

        Client client1 = new Client(f);
        client1.ConnectToServer("127.0.0.1", 8888);
        new Thread(client1).start();

        Client client2 = new Client(f);
        client2.ConnectToServer("127.0.0.1", 8888);
        new Thread(client2).start();

        Thread.sleep(10000);
    }
};
