package br.com.souenzzo.aoc21;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
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

    static <T> BiConsumer<T, Consumer<List<T>>> partition(Integer n) {
        var arr = new LinkedList<T>();
        return (T el, Consumer<List<T>> consumer) -> {
            arr.add(el);
            if (arr.size() >= n) {
                consumer.accept(List.copyOf(arr));
                arr.removeFirst();
            }
        };
    }

}

public class Main {
    static String Answer01() throws IOException, InterruptedException {
        var result = Utils.lines(aoc.inputOfTheDay(1))
                .map(Long::parseLong)
                .mapMulti(Utils.partition(2))
                .mapToLong(el -> el.get(0) > el.get(1) ? 0 : 1)
                .reduce(0, Long::sum);
        return String.valueOf(result);
    }
    static String Answer01extra() throws IOException, InterruptedException {
        var result = Utils.lines(aoc.inputOfTheDay(1))
                .map(Long::parseLong)
                .mapMulti(Utils.partition(3))
                .mapToLong(el -> el.get(0) > el.get(1) ? 0 : 1)
                .reduce(0, Long::sum);
        return String.valueOf(result);
    }

    static String Answer02() throws IOException, InterruptedException {
        aoc.inputOfTheDay(2);
        return "";
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.printf("01: %s%n", Answer01());
        System.out.printf("01 - extra: %s%n", Answer01extra());
        System.out.printf("02: %s%n", Answer02());
    }
}
