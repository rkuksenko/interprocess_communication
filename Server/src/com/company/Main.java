package com.company;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ArrayList;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

class Server extends Thread {
    Server(String ipAddr, int port, int clientsNumber) throws IOException {
        m_inetAddress = new InetSocketAddress(ipAddr, port);
        m_clients = new ArrayList<SocketChannel>();
        m_clientsNumber = clientsNumber;

        init();

        new Thread(this).start();
    }
    boolean IsReady() throws InterruptedException {
        m_semaphore.acquire();
        boolean isReady = m_clients.size() >= m_clientsNumber;
        m_semaphore.release();

        return isReady;
    }
    void SendArgs(int[] xArgs) throws IOException {
        System.out.println("[Server::SendArgs()] Going to send message to client...");

        for (int i = 0; i < m_clientsNumber; i++) {
            sendArgsToClient(xArgs[i], i);
        }
    }
    void HandleClientResponse() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int numRead = 0;
        do {
            numRead = m_clients.get(0).read(buffer);
        } while (numRead == 0);

        int response = parseArguments(buffer, numRead);

        System.out.println("[Server::HandleClientResponse()] received response from a client: " + response);
        ByteBuffer buffer2 = ByteBuffer.allocate(1024);
        int numRead2 = 0;
        do {
            numRead2 = m_clients.get(1).read(buffer2);
        } while (numRead2 == 0);

        int response2 = parseArguments(buffer2, numRead2);

        System.out.println("[Server::HandleClientResponse()] received response from a client: " + response2);
    }

    private void sendArgsToClient(Integer x, int clientNumber) throws IOException {
        if (clientNumber + 1 > m_clients.size()|| m_clients.get(clientNumber) == null) {
            System.out.println("[Server::sendArgsToClient()] Client number is incorrect or client is disconnected");
            return;
        }

        byte[] message = new String(x.toString()).getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.wrap(message);
        m_clients.get(clientNumber).write(buffer);

        System.out.println("[Server::sendArgsToClient()] Message: \"" + x.toString() + "\" was sent to client: "
                + m_clients.get(clientNumber).getRemoteAddress().toString());
    }
    private void init() throws IOException {
        m_serverSocketChannel = ServerSocketChannel.open();
        m_serverSocketChannel.configureBlocking(true);
        m_serverSocketChannel.socket().bind(m_inetAddress);
    }
    @Override
    public void run() {
        try {
            runWorker();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
    private void runWorker() throws IOException, InterruptedException {
        System.out.println("[Server::runWorker()] Server started...");

        while (true) {
            SocketChannel client = m_serverSocketChannel.accept();

            if (client != null) {
                this.acceptNewConnection(client);
            }
        }
    }
    private int parseArguments(ByteBuffer buf, int size) {
        System.out.println("[Server::parseArguments()] Parsing received param...");

        buf.flip();
        byte[] data = new byte[size];
        System.arraycopy(buf.array(), 0, data, 0, size);
        String[] splitedMessages = new String(data).split(" ");

        int x = Integer.parseInt(splitedMessages[0]);
        System.out.println("[Client::parseArguments(" + Thread.currentThread().getId() + ")] received x: " + x);
        return x;


    }
    private void acceptNewConnection(SocketChannel newClient) throws IOException, InterruptedException {
        System.out.println("[Server::acceptNewConnection()] Connection established with: " + newClient.getRemoteAddress());

        m_semaphore.acquire();
        this.m_clients.add(newClient);
        m_semaphore.release();
    }

    private ServerSocketChannel m_serverSocketChannel;
    private ArrayList<SocketChannel> m_clients;
    private InetSocketAddress m_inetAddress;
    private Integer m_clientsNumber;
    private static Semaphore m_semaphore = new Semaphore(1);
};

class ConsoleManager {
    static void ShowOptions() {
        System.out.println("1. f finish before g with non zero value");
        System.out.println("2. g finish before f with non zero value");
        System.out.println("3. f finish with zero value, g hangs");
        System.out.println("4. g finish with zero value, f hangs");
        System.out.println("5. f finish with non-zero value, g hangs");
        System.out.println("6. g finish with non-zero value, f hangs");
        System.out.println("0. Exit");
    }
    static int ReadOption() {
        System.out.println("Input your choice: ");
        Scanner in = new Scanner(System.in);

        return in.nextInt();
    }
};

class ValueGenerator {
    static int[] GenerateArguments(Integer option) {
        switch (option) {
            case 1:
                return new int[] {1, 4};
            case 2:
                return new int[] {4, 1};
            case 3:
                return new int[] {2, 3};
            case 4:
                return new int[] {3, 2};
            case 5:
                return new int[] {1, 3};
            case 6:
                return new int[] {3, 1};
            default:
                System.out.println("ValueConverter::GetArguments() no such value: " + option);
                return new int[] {-1, -1};
        }
    }
}


public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        Server server = new Server("127.0.0.1", 8888, 2);
        Thread.sleep(10); // wait until server started properly

        System.out.println("[Server] Waiting for a clients...");

        while (!server.IsReady()) {
            System.out.println("...");
            Thread.sleep(1500);
        }

        System.out.println("[Server] Clients connected, You can manage their functions execution:");
        ConsoleManager.ShowOptions();
        int choice = ConsoleManager.ReadOption();

        if (choice == 0) {
            System.out.println("[Server] Exiting...");
            return;
        }

        server.SendArgs(ValueGenerator.GenerateArguments(choice));
        server.HandleClientResponse();
    }
}
