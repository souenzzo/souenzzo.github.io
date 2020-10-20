package br.com.souenzzo.inter;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class DSHttp {
    public int port;
    private ServerSocket ss;
    private ExecutorService es;
    private Socket client;
    private final Runnable watch = () -> {
        try {
            client = ss.accept();
            es.submit(this.handler(client));
        } catch (IOException e) {
        }
        es.submit(this.watch);
    };

    public void start() throws IOException {
        es = Executors.newFixedThreadPool(2);
        ss = new ServerSocket(port);
        es.submit(this.watch);
    }

    @org.jetbrains.annotations.NotNull
    @org.jetbrains.annotations.Contract(pure = true)
    private Runnable handler(Socket client) {
        return () -> {

        };
    }

    public void setSs(ServerSocket ss) {
        this.ss = ss;
    }

    public void setEs(ExecutorService es) {
        this.es = es;
    }

    public void setClient(Socket client) {
        this.client = client;
    }
}

public class Main {
    public static void main(String[] argv) {
    }
}
