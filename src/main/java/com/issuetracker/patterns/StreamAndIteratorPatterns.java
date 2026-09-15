package com.issuetracker.patterns;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.*;

/**
 * Java's answers to `IEnumerable<T>` + `yield return` and `IAsyncEnumerable<T>`.
 * Java has no `yield return`, so laziness is expressed with Iterator / Stream / Spliterator.
 */
@Component
public class StreamAndIteratorPatterns {

  /** 1. Eager collection (the naive translation of a yield method). */
  public List<Integer> eagerSquares() {
    int[] seed = {1, 2, 3, 4, 5};
    List<Integer> out = new ArrayList<>();
    for (int i : seed) out.add(i * i);
    return out;
  }

  /** 2. Lazy Stream — closest to `IEnumerable<T>` with deferred execution. */
  public Stream<Integer> lazySquares() {
    return IntStream.rangeClosed(1, 5).mapToObj(i -> i * i);
  }

  /** 3. Infinite generator + short-circuit (Stream.iterate == yield in a loop). */
  public List<Integer> fibonacci(int count) {
    return Stream.iterate(new int[]{0, 1}, f -> new int[]{f[1], f[0] + f[1]})
        .limit(count)
        .map(f -> f[0])
        .collect(Collectors.toList());
  }

  /** 4. Hand-written Iterator — the literal state machine the C# compiler generates. */
  public Iterable<Integer> handWrittenIterator(int upTo) {
    return () -> new Iterator<>() {
      private int current = 0;
      @Override public boolean hasNext() { return current < upTo; }
      @Override public Integer next() {
        if (!hasNext()) throw new NoSuchElementException();
        int value = current++;
        return value * value;
      }
    };
  }

  /** 5. Custom Spliterator — parallel-capable lazy source. */
  public List<String> customSpliterator() {
    Spliterator<String> sp = new Spliterators.AbstractSpliterator<>(3, Spliterator.ORDERED) {
      private int i = 0;
      @Override public boolean tryAdvance(java.util.function.Consumer<? super String> action) {
        if (i >= 3) return false;
        action.accept("item-" + i++);
        return true;
      }
    };
    return StreamSupport.stream(sp, false).collect(Collectors.toList());
  }

  /** 6. Generator via Supplier (pull-based, stateful closure). */
  public List<String> supplierGenerator() {
    int[] counter = {0};
    Supplier<String> gen = () -> "gen" + counter[0]++;
    return Stream.generate(gen).limit(4).collect(Collectors.toList());
  }

  /** 7. Async stream — the IAsyncEnumerable equivalent: producer thread + BlockingQueue. */
  public List<Integer> asyncStream() throws InterruptedException {
    BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(8);
    Integer poison = -1;
    CompletableFuture.runAsync(() -> {
      for (int i = 0; i < 5; i++) {
        AsyncPatterns.sleep(1);
        try { queue.put(i); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
      }
      try { queue.put(poison); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    });
    List<Integer> received = new ArrayList<>();
    while (true) {
      Integer v = queue.take();
      if (v.equals(poison)) break;
      received.add(v);
    }
    return received;
  }

  /** 8. Grouping / partitioning / reducing collectors (LINQ GroupBy, Aggregate). */
  public Map<String, Object> collectors() {
    List<String> words = List.of("alpha", "beta", "gamma", "delta", "epsilon");
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("groupedByLength", words.stream().collect(Collectors.groupingBy(String::length)));
    out.put("partitioned", words.stream().collect(Collectors.partitioningBy(w -> w.length() > 4)));
    out.put("joined", words.stream().collect(Collectors.joining(", ", "[", "]")));
    out.put("summary", words.stream().collect(Collectors.summarizingInt(String::length)).toString());
    out.put("reduced", words.stream().reduce("", (a, b) -> a.isEmpty() ? b : a + "|" + b));
    return out;
  }

  /** 9. Parallel stream (PLINQ). */
  public long parallelStream() {
    return IntStream.rangeClosed(1, 1_000).parallel().filter(i -> i % 3 == 0).count();
  }

  /** 10. flatMap + distinct + sorted pipeline. */
  public List<String> pipeline() {
    List<List<String>> nested = List.of(List.of("b", "a"), List.of("c", "a"));
    return nested.stream().flatMap(List::stream).distinct().sorted().collect(Collectors.toList());
  }
}
