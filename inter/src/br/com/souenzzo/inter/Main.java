package br.com.souenzzo.inter;

import java.io.*;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

record Request(String method, String path, String version,
               Map<String, List<String>> headers,
               InputStream body) {
    public static Request from(InputStream is) throws IOException {
        var pis = new PushbackInputStream(is);
        return new Request(
                readMethod(pis),
                readPath(pis),
                readVersion(pis),
                readHeaders(pis),
                is
        );
    }

    private static Map<String, List<String>> readHeaders(InputStream is) {
        return new HashMap<>();
    }

    private static String readVersion(PushbackInputStream pis) throws IOException {
        var sb = new StringBuilder();
        while (true) {
            var x = pis.read();
            if (Character.isWhitespace(x)) {
                return sb.toString();
            }
            sb.append(x);
        }
    }

    private static String readMethod(PushbackInputStream pis) throws IOException {
        var sb = new StringBuilder();
        while (true) {
            var x = pis.read();
            if (Character.isWhitespace(x)) {
                return sb.toString();
            }
            sb.append(x);
        }
    }

    private static String readPath(PushbackInputStream pis) throws IOException {
        var sb = new StringBuilder();
        while (true) {
            var x = pis.read();
            if (Character.isWhitespace(x)) {
                return sb.toString();
            }
            sb.append(x);
        }
    }
}

class EchoHttpServer {
    void start() throws IOException {
        try (final var ss = new ServerSocket(8080)) {
            while (true) {
                final var sc = ss.accept();
                final var is = sc.getInputStream();
                final var os = sc.getOutputStream();
                // final var request = new String(is.readAllBytes());
                // System.out.println(request);
                os.write(String.join("\r\n",
                        "HTTP/1.1 200 OK",
                        "Date: Tue, 20 Oct 2020 21:16:08 GMT",
                        "Server: dshttp/0",
                        "",
                        "ok!").getBytes());
                os.close();
                is.close();
                sc.close();
            }

        }
    }
}

public class Main {
    public static void main(String[] argv) throws IOException {
        (new EchoHttpServer()).start();
    }
}

