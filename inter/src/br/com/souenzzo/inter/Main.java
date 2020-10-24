package br.com.souenzzo.inter;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class Request extends OutputStream {
    public String method;
    public String path;
    public String version;
    public Map<String, List<String>> headers = new HashMap<String, List<String>>();

    public boolean withinCR = false;
    public String currentHeaderKey;

    public enum STATE {METHOD, PATH, VERSION, HEADERS, BODY}

    public STATE currentState = STATE.METHOD;
    public OutputStream baos = new ByteArrayOutputStream();

    public void writeMethod(int b) throws IOException {
        if (Character.isWhitespace(b)) {
            method = baos.toString();
            baos = new ByteArrayOutputStream();
            currentState = STATE.PATH;
        } else {
            baos.write(b);
        }
    }

    public void writePath(int b) throws IOException {
        if (Character.isWhitespace(b)) {
            path = baos.toString();
            baos = new ByteArrayOutputStream();
            currentState = STATE.VERSION;
        } else {
            baos.write(b);
        }
    }

    public void writeVersion(int b) throws IOException {
        switch (b) {
            case '\r' -> {
                withinCR = true;
                baos.write(b);
            }
            case '\n' -> {
                if (withinCR) {
                    withinCR = false;
                    version = baos.toString();
                    baos = new ByteArrayOutputStream();
                    currentState = STATE.HEADERS;
                } else {
                    baos.write(b);
                }
            }
            default -> {
                withinCR = false;
                baos.write(b);
            }
        }
    }

    private void writeHeaders(int b) throws IOException {
        switch (b) {
            case '\r' -> {
                withinCR = true;
                baos.write(b);
            }
            case '\n' -> {
                if (withinCR) {
                    withinCR = false;
                    if (Objects.isNull(currentHeaderKey)) {
                        currentState = STATE.BODY;

                    } else {
                        if (headers.containsKey(currentHeaderKey)) {
                            headers.get(currentHeaderKey).add(baos.toString());
                        } else {
                            headers.put(currentHeaderKey, List.of(baos.toString()));
                        }
                    }
                    baos = new ByteArrayOutputStream();
                    currentHeaderKey = null;
                } else {
                    baos.write(b);
                }
            }
            case ':' -> {
                currentHeaderKey = baos.toString();
                baos = new ByteArrayOutputStream();
            }
            default -> {
                withinCR = false;
                baos.write(b);
            }
        }
    }

    public void write(int b) throws IOException {
        switch (currentState) {
            case METHOD -> writeMethod(b);
            case PATH -> writePath(b);
            case VERSION -> writeVersion(b);
            case HEADERS -> writeHeaders(b);
            default -> throw new IllegalStateException();
        }
    }
}

public class Main {
    public static void main(String[] argv) throws IOException {
        var req = new Request();
        req.write(String.join("\r\n",
                "GET /foo HTTP/1.1",
                "Content-Length: 123",
                "\r\n").getBytes());
        System.out.println("method");
        System.out.println(req.method);
        System.out.println("path");
        System.out.println(req.path);
        System.out.println("version");
        System.out.println(req.version);
        System.out.println("headers");
        System.out.println(req.headers);
        System.out.println(req.currentState);
    }
}

