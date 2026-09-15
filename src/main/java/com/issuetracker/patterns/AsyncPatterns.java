package com.issuetracker.patterns;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Every flavour of "how a method can be declared / executed" in Java.
 *
 * .NET reference mapping:
 *   Task<T>                 -> CompletableFuture<T>
 *   async/await             -> thenApply / thenCompose / join
 *   ConfigureAwait(false)   -> supplying an explicit Executor
 *   IAsyncEnumerable<T>     -> BlockingQueue / Stream / SseEmitter
 *   IEnumerable<T> + yield  -> Stream.iterate / Iterable (see StreamAndIteratorPatterns)
 */
@Component
public class AsyncPatterns {

  /* ------------------------------------------------------------------ *
   * 1. Plain synchronous method
   * ------------------------------------------------------------------ */
  public String standardSync() {
    return "sync";
  }

  /* 2. Synchronous but delegating to a private helper (controller -> service -> internal fn) */
  public String syncDelegating() {
    return decorate(standardSync());
  }

  private String decorate(String input) {
    return input + "_decorated";
  }

  /* 3. static method */
  public static String staticHelper() {
    return "static";
  }

  /* 4. final method (cannot be overridden) */
  public final String finalMethod() {
    return "final";
  }

  /* 5. synchronized method */
  public synchronized String synchronizedMethod() {
    return "synchronized";
  }

  /* 6. varargs */
  public String varargs(String... parts) {
    return String.join("-", parts);
  }

  /* 7. generic method with bounded type parameter */
  public <T extends Comparable<T>> T maxOf(T a, T b) {
    return a.compareTo(b) >= 0 ? a : b;
  }

  /* ------------------------------------------------------------------ *
   * ASYNC
   * ------------------------------------------------------------------ */

  /** 8. Equivalent of `async Task<string> StandardAsync()`. */
  public CompletableFuture<String> standardAsync() {
    return CompletableFuture.supplyAsync(() -> {
      sleep(10);
      return "done";
    });
  }

  /** 9. Equivalent of ConfigureAwait(false): run on an explicit executor, never the caller thread. */
  public CompletableFuture<String> withExplicitExecutor() {
    ExecutorService pool = Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r, "explicit-pool");
      t.setDaemon(true);
      return t;
    });
    return CompletableFuture
        .supplyAsync(() -> { sleep(10); return "configured"; }, pool)
        .whenComplete((v, e) -> pool.shutdown());
  }

  /** 10. Composition: await one async result then continue (thenCompose == await chaining). */
  public CompletableFuture<String> complexAsync() {
    return standardAsync().thenCompose(a ->
        CompletableFuture.supplyAsync(() -> a + "_complex"));
  }

  /** 11. Fan-out / fan-in (Task.WhenAll). */
  public CompletableFuture<List<String>> whenAll() {
    List<CompletableFuture<String>> futures = IntStream.range(0, 4)
        .mapToObj(i -> CompletableFuture.supplyAsync(() -> { sleep(5); return "task" + i; }))
        .collect(Collectors.toList());
    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .thenApply(v -> futures.stream().map(CompletableFuture::join).collect(Collectors.toList()));
  }

  /** 12. Race (Task.WhenAny). */
  public CompletableFuture<Object> whenAny() {
    return CompletableFuture.anyOf(
        CompletableFuture.supplyAsync(() -> { sleep(30); return "slow"; }),
        CompletableFuture.supplyAsync(() -> { sleep(5); return "fast"; }));
  }

  /** 13. Async with error recovery (try/catch across the await boundary). */
  public CompletableFuture<String> asyncWithFallback(boolean fail) {
    return CompletableFuture
        .<String>supplyAsync(() -> {
          if (fail) throw new IllegalStateException("boom");
          return "ok";
        })
        .exceptionally(ex -> "recovered:" + ex.getCause().getMessage());
  }

  /** 14. Timeout (CancellationToken-ish). */
  public CompletableFuture<String> asyncWithTimeout() {
    return CompletableFuture.supplyAsync(() -> { sleep(500); return "never"; })
        .completeOnTimeout("timed-out", 50, TimeUnit.MILLISECONDS);
  }

  /** 15. Spring's @Async — the framework hands the call to a TaskExecutor. */
  @Async("patternsExecutor")
  public CompletableFuture<String> springAsync() {
    sleep(10);
    return CompletableFuture.completedFuture("spring-async@" + Thread.currentThread().getName());
  }

  /** 16. Fire and forget. */
  @Async("patternsExecutor")
  public void fireAndForget(String message) {
    sleep(5);
    System.out.println("[fire-and-forget] " + message);
  }

  /** 17. Callable — Servlet async, container waits on the returned value. */
  public Callable<String> callableEndpointBody() {
    return () -> { sleep(10); return "callable"; };
  }

  /** 18. DeferredResult — completed by some other thread later. */
  public DeferredResult<String> deferred() {
    DeferredResult<String> result = new DeferredResult<>(Duration.ofSeconds(2).toMillis(), "deferred-timeout");
    CompletableFuture.runAsync(() -> { sleep(20); result.setResult("deferred"); });
    return result;
  }

  /** 19. Virtual-thread style: one task per (platform) thread executor. */
  public List<String> perTaskExecutor() {
    ExecutorService exec = Executors.newCachedThreadPool();
    try {
      List<Future<String>> futures = IntStream.range(0, 3)
          .mapToObj(i -> exec.submit(() -> "vt" + i))
          .collect(Collectors.toList());
      return futures.stream().map(f -> {
        try { return f.get(); } catch (Exception e) { throw new CompletionException(e); }
      }).collect(Collectors.toList());
    } finally {
      exec.shutdown();
    }
  }

  /** 20. Scheduled / delayed execution. */
  public CompletableFuture<String> delayed() {
    ScheduledExecutorService s = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "delayer"); t.setDaemon(true); return t;
    });
    CompletableFuture<String> f = new CompletableFuture<>();
    s.schedule(() -> f.complete("delayed"), 20, TimeUnit.MILLISECONDS);
    f.whenComplete((v, e) -> s.shutdown());
    return f;
  }

  static void sleep(long ms) {
    try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
  }
}
