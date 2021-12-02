package br.com.souenzzo.aoc21;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class aoc {
    public static InputStream inputOfTheDay(Number d) throws IOException, InterruptedException {
        var target = new File("day/%d/input".formatted(d));
        if (!target.exists()) {
            var client = HttpClient.newHttpClient();
            var request = HttpRequest.newBuilder(
                            URI.create("https://adventofcode.com/2021/day/%d/input".formatted(d)))
                    .header("Cookie", "session=%s".formatted(System.getenv("AOC_TOKEN")))
                    .build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
            target.getParentFile().mkdirs();
            new FileWriter(target)
                    .append(response)
                    .close();
        }
        return new FileInputStream(target);

    }
}

class Utils {
    public static Stream<String> lines(InputStream in) {
        return (new BufferedReader(new InputStreamReader(in))).lines();
    }
}

public class Main {
    static String Answer01() throws IOException, InterruptedException {
        var result = new LinkedList<List<Long>>();
        var lastN = new AtomicLong();
        var initial = new AtomicBoolean(true);
        Utils.lines(aoc.inputOfTheDay(1))
                .map(Long::parseLong)
                .collect(Collectors.toList())
                .forEach(el -> {
                    if (initial.get()) {
                        initial.set(false);
                    } else {
                        result.push(List.of(lastN.get(), el));
                    }
                    lastN.set(el);
                });
        return String.valueOf(result.stream()
                .mapToLong(el -> el.get(0) > el.get(1) ? 0 : 1)
                .reduce(0, Long::sum));
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.printf("01: %s%n", Answer01());
    }
}
