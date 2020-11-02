package br.com.souenzzo.inter;

import org.msgpack.io.Output;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

record HTTPResponse(Number status,
                    Map<String, List<String>> headers,
                    InputStream body) {
    public static Map<String, List<String>> EMPTY_HEADERS = new HashMap<String, List<String>>();

    public HTTPResponse(Number status) {
        this(status, EMPTY_HEADERS, ByteArrayInputStream.nullInputStream());
    }

    public HTTPResponse(Number status, String body) {
        this(status, EMPTY_HEADERS, new ByteArrayInputStream(body.getBytes()));
    }
}

class Request extends OutputStream {
    public String method;
    public String path;
    public String version;
    public Map<String, List<String>> headers = new HashMap<String, List<String>>();

    public boolean withinCR = false;
    public String currentHeaderKey;
    public Integer contentLength;

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

    private void gotoBody() {
        currentState = STATE.BODY;
        try {
            contentLength = Integer.parseInt(headers.get("Content-Length").iterator().next());
        } catch (Throwable ex) {
            contentLength = null;
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
                        gotoBody();
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

interface IAccount {
    public String getFirstName();
}

abstract class AAccount implements IAccount {
    public abstract String getFirstName();

    public abstract String getLastName();

    public String getFullName() {
        return String.join(" ", getFirstName(), getLastName());
    }
}

record Account2(String firstName,
                String lastName) {
    public String getFullName() {
        return String.join(" ", firstName, lastName);
    }
}

public class Main {
    public static void main(String[] argv) {
    }
}
