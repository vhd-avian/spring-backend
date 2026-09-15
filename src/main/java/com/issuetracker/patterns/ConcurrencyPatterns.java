package com.issuetracker.patterns;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Threading / locking / task function shapes.
 */
@Component
public class ConcurrencyPatterns {

  private final Object monitor = new Object();
  private final ReentrantLock lock = new ReentrantLock();
  private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
  private final Semaphore semaphore = new Semaphore(2);
  private final AtomicLong counter = new AtomicLong();
  private volatile boolean flag;
  private transient int notSerialized;

  private static final ThreadLocal<StringBuilder> SCRATCH =
      ThreadLocal.withInitial(StringBuilder::new);

  /** synchronized instance method */
  public synchronized long synchronizedMethod() {
    return counter.incrementAndGet();
  }

  /** synchronized static method */
  public static synchronized String synchronizedStatic() {
    return "class-locked";
  }

  /** synchronized block on an explicit monitor */
  public long synchronizedBlock() {
    synchronized (monitor) {
      return counter.addAndGet(2);
    }
  }

  /** explicit lock / unlock in finally */
  public long withReentrantLock() {
    lock.lock();
    try {
      return counter.incrementAndGet();
    } finally {
      lock.unlock();
    }
  }

  /** tryLock with timeout */
  public String withTryLock() throws InterruptedException {
    if (lock.tryLock(50, TimeUnit.MILLISECONDS)) {
      try {
        return "acquired";
      } finally {
        lock.unlock();
      }
    }
    return "busy";
  }

  /** read/write lock pair */
  public String withReadWriteLock(String value) {
    rwLock.writeLock().lock();
    try {
      SCRATCH.get().setLength(0);
      SCRATCH.get().append(value);
    } finally {
      rwLock.writeLock().unlock();
    }
    rwLock.readLock().lock();
    try {
      return SCRATCH.get().toString();
    } finally {
      rwLock.readLock().unlock();
    }
  }

  /** semaphore-guarded section */
  public String withSemaphore() throws InterruptedException {
    semaphore.acquire();
    try {
      return "permit-held:" + semaphore.availablePermits();
    } finally {
      semaphore.release();
    }
  }

  /** volatile write / read pair */
  public boolean toggleVolatile() {
    flag = !flag;
    notSerialized++;
    return flag;
  }

  /** Runnable implemented as a lambda */
  public final Runnable lambdaRunnable = () -> counter.incrementAndGet();

  /** Runnable as an anonymous class */
  public final Runnable anonymousRunnable = new Runnable() {
    @Override
    public void run() {
      counter.incrementAndGet();
    }
  };

  /** Thread subclass overriding run() */
  public static final class WorkerThread extends Thread {
    private final AtomicLong sink;

    public WorkerThread(AtomicLong sink) {
      super("pattern-worker");
      this.sink = sink;
      setDaemon(true);
    }

    @Override
    public void run() {
      sink.incrementAndGet();
    }
  }

  /** raw thread start / join */
  public long rawThread() throws InterruptedException {
    WorkerThread t = new WorkerThread(counter);
    t.start();
    t.join(500);
    return counter.get();
  }

  /** Callable submitted to a pooled executor */
  public String pooledCallable() throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      Callable<String> task = () -> "callable:" + Thread.currentThread().getName();
      return pool.submit(task).get(1, TimeUnit.SECONDS);
    } finally {
      pool.shutdown();
    }
  }

  /** invokeAll: many callables, ordered results */
  public List<String> invokeAll() throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(3);
    try {
      List<Callable<String>> tasks = new ArrayList<>();
      for (int i = 0; i < 3; i++) {
        int n = i;
        tasks.add(() -> "task-" + n);
      }
      List<String> out = new ArrayList<>();
      for (var f : pool.invokeAll(tasks)) {
        out.add(f.get());
      }
      return out;
    } finally {
      pool.shutdown();
    }
  }

  /** CountDownLatch coordination */
  public String withLatch() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(3);
    ExecutorService pool = Executors.newCachedThreadPool();
    try {
      for (int i = 0; i < 3; i++) {
        pool.execute(latch::countDown);
      }
      boolean done = latch.await(1, TimeUnit.SECONDS);
      return done ? "all-finished" : "timeout";
    } finally {
      pool.shutdown();
    }
  }

  /** ForkJoin RecursiveTask - divide and conquer */
  public static final class SumTask extends RecursiveTask<Long> {
    private static final long serialVersionUID = 1L;
    private final long[] data;
    private final int from;
    private final int to;

    public SumTask(long[] data, int from, int to) {
      this.data = data;
      this.from = from;
      this.to = to;
    }

    @Override
    protected Long compute() {
      if (to - from <= 4) {
        long sum = 0;
        for (int i = from; i < to; i++) sum += data[i];
        return sum;
      }
      int mid = (from + to) >>> 1;
      SumTask left = new SumTask(data, from, mid);
      SumTask right = new SumTask(data, mid, to);
      left.fork();
      return right.compute() + left.join();
    }
  }

  public long forkJoinSum() {
    long[] data = new long[32];
    for (int i = 0; i < data.length; i++) data[i] = i;
    return ForkJoinPool.commonPool().invoke(new SumTask(data, 0, data.length));
  }

  /** scheduled one-shot task */
  public String scheduled() throws Exception {
    var scheduler = Executors.newSingleThreadScheduledExecutor();
    try {
      return scheduler.schedule(() -> "ran-later", 20, TimeUnit.MILLISECONDS).get();
    } finally {
      scheduler.shutdown();
    }
  }

  /** double-checked locking with a volatile holder */
  private volatile String lazyValue;

  public String doubleCheckedLazy() {
    String local = lazyValue;
    if (local == null) {
      synchronized (this) {
        local = lazyValue;
        if (local == null) {
          lazyValue = local = "computed-once";
        }
      }
    }
    return local;
  }

  public Map<String, Object> summary() throws Exception {
    Map<String, Object> out = new HashMap<>();
    out.put("synchronizedMethod", synchronizedMethod());
    out.put("synchronizedStatic", synchronizedStatic());
    out.put("synchronizedBlock", synchronizedBlock());
    out.put("reentrantLock", withReentrantLock());
    out.put("tryLock", withTryLock());
    out.put("readWriteLock", withReadWriteLock("shared"));
    out.put("semaphore", withSemaphore());
    out.put("volatileToggle", toggleVolatile());
    out.put("rawThread", rawThread());
    out.put("pooledCallable", pooledCallable());
    out.put("invokeAll", invokeAll());
    out.put("latch", withLatch());
    out.put("forkJoinSum", forkJoinSum());
    out.put("scheduled", scheduled());
    out.put("doubleCheckedLazy", doubleCheckedLazy());
    return out;
  }
}
