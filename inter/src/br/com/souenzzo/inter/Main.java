package br.com.souenzzo.inter;

import java.io.*;
import java.net.ServerSocket;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

record Request(Integer serverPort,
               String serverName,
               String remoteAddr,
               String uri,
               String queryString,
               String scheme,
               String requestMethod,
               String protocol,
               Map<String, String> headers,
               InputStream body) {
}

record Response(Integer status, Map<String, String> headers, String body) {
}

public class Main {
    static Pattern reqLinePattern = Pattern.compile("([^\s]+)\s([^\s]+)\s([^\s]+)");

    static Response handler(Request r) {
        return new Response(200, new HashMap<String, String>(), "");
    }

    static List<String> match(Pattern pattern, String str) {
        var m = pattern.matcher(str);
        if (!m.find()) {
            return null;
        }
        var ret = new LinkedList<String>();
        for (var i = 0; i < m.groupCount(); i++) {
            ret.add(m.group(i + 1));
        }
        return ret;

    }

    public static void main(String[] argv) throws Exception {
        try (var ss = new ServerSocket(8080);
             var cs = ss.accept();
             var in = cs.getInputStream();
             var isr = new InputStreamReader(in);
             var br = new BufferedReader(isr);
             var out = cs.getOutputStream();
             var osw = new OutputStreamWriter(out);) {
            var reqLine = br.readLine();
            var reqLines = match(reqLinePattern, reqLine);
            if (Objects.isNull(reqLines)) {
                throw new IllegalStateException("can't");
            }
            var requestMethod = reqLines.get(0);
            var path = reqLines.get(1).split("\\?", 2);
            var protocol = reqLines.get(2);
            var rawHeaders = new LinkedList<String>();
            do {
                rawHeaders.add(br.readLine());
            } while (!rawHeaders.getLast().isEmpty());
            var headers = new HashMap<String, String>();
            rawHeaders.stream().filter(s -> !s.isEmpty())
                    .forEach(e -> {
                        var kv = e.split(":\\s{0,}", 2);
                        if (kv.length != 2) {
                            throw new IllegalStateException("can't");
                        }
                        headers.put(kv[0], kv[1]);
                    });
            var response = handler(new Request(ss.getLocalPort(),
                    "localhost",
                    "127.0.0.1",
                    path[0],
                    path.length == 2 ? path[1] : null,
                    "http",
                    requestMethod,
                    protocol,
                    headers,
                    in));

            osw.write(String.format("HTTP/1.1 %s OK\r\n", response.status()));
            var responseHeaders = new HashMap<String, String>();
            responseHeaders.put("Server", "dshttpd");
            responseHeaders.put("Date", DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC)));
            responseHeaders.putAll(response.headers());


            for (var entry : responseHeaders.entrySet()) {
                osw.write(entry.getKey());
                osw.write(": ");
                osw.write(entry.getValue());
                osw.write("\r\n");
            }
            osw.write("\r\n");
            osw.write(response.body());


        }

    }
}

// package com.example;
interface IGreet {
    String greet();
}

class Main {
    public static void main() {

        // Criando uma implementação
        var impl = new IGreet() {
            @Override
            public String greet() {
                return "hello";
            }
        };

        // Chamando a implementação
        impl.greet();

    }
}


