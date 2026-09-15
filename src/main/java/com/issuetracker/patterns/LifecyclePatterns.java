package com.issuetracker.patterns;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.lang.ref.Cleaner;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Object lifecycle / construction / disposal function shapes.
 * Java counterpart of the .NET IDisposable + finalizer + static ctor example.
 */
@Component
public class LifecyclePatterns {

  /** static field with initializer expression */
  private static final AtomicInteger INSTANCES = new AtomicInteger();

  /** static initializer block (the Java "static constructor") */
  static {
    System.out.println("LifecyclePatterns static init");
  }

  /** second static block - legal, runs in order */
  static {
    INSTANCES.set(0);
  }

  private final List<String> log = new ArrayList<>();
  private int id;

  /** instance initializer block (runs before every constructor body) */
  {
    this.log.add("instance-init");
  }

  /** no-arg constructor */
  public LifecyclePatterns() {
    this(INSTANCES.incrementAndGet());
  }

  /** overloaded constructor with explicit this(...) delegation */
  public LifecyclePatterns(int id) {
    this.id = id;
    this.log.add("ctor(" + id + ")");
  }

  /** generic constructor - a constructor with its own type parameter */
  public <T extends Number> LifecyclePatterns(T seed, boolean unusedMarker) {
    this(seed.intValue());
    this.log.add("generic-ctor:" + unusedMarker);
  }

  /** Spring lifecycle callback */
  @PostConstruct
  void afterPropertiesSetLikeHook() {
    log.add("post-construct");
  }

  /** Spring destruction callback */
  @PreDestroy
  void beforeDestroy() {
    log.add("pre-destroy");
  }

  public List<String> lifecycleLog() {
    return List.copyOf(log);
  }

  public int id() {
    return id;
  }

  // ---------------------------------------------------------------- resources

  /** AutoCloseable resource - the modern Java replacement for IDisposable */
  public static class ManagedResource implements AutoCloseable {
    private final String name;
    private boolean closed;

    public ManagedResource(String name) {
      this.name = name;
    }

    public String use() {
      if (closed) throw new IllegalStateException("closed");
      return "using:" + name;
    }

    @Override
    public void close() {
      this.closed = true;
    }

    public boolean isClosed() {
      return closed;
    }
  }

  /** Closeable that declares a checked exception on close() */
  public static class ThrowingResource implements AutoCloseable {
    @Override
    public void close() throws Exception {
      // no-op, but the throws clause changes the call-site shape
    }
  }

  /** try-with-resources, single resource */
  public String useResource() {
    try (ManagedResource r = new ManagedResource("single")) {
      return r.use();
    }
  }

  /** try-with-resources, multiple + effectively-final resource variable */
  public String useResources() throws Exception {
    ManagedResource existing = new ManagedResource("pre-existing");
    try (existing; ManagedResource r2 = new ManagedResource("second"); ThrowingResource r3 = new ThrowingResource()) {
      return r2.use() + "|" + existing.use() + "|" + (r3 != null);
    } finally {
      log.add("resources-closed");
    }
  }

  /** try / catch (multi-catch) / finally with a return inside finally-free path */
  @SuppressWarnings("finally")
  public String guardedCall(String mode) {
    try {
      if ("npe".equals(mode)) throw new NullPointerException("boom");
      if ("iae".equals(mode)) throw new IllegalArgumentException("bad");
      return "ok";
    } catch (NullPointerException | IllegalArgumentException e) {
      return "caught:" + e.getClass().getSimpleName();
    } catch (RuntimeException e) {
      throw new IllegalStateException(e);
    } finally {
      log.add("finally:" + mode);
    }
  }

  // ------------------------------------------------------------ cleaner + gc

  private static final Cleaner CLEANER = Cleaner.create();

  /** modern finalizer replacement: static Runnable state holder */
  static final class CleanupState implements Runnable {
    private final String tag;

    CleanupState(String tag) {
      this.tag = tag;
    }

    @Override
    public void run() {
      // released native-ish resource for tag
    }

    String tag() {
      return tag;
    }
  }

  /** object registering itself with a Cleaner */
  public static class CleanableThing {
    private final CleanupState state;
    private final Cleaner.Cleanable cleanable;

    public CleanableThing(String tag) {
      this.state = new CleanupState(tag);
      this.cleanable = CLEANER.register(this, state);
    }

    public String tag() {
      return state.tag();
    }

    public void release() {
      cleanable.clean();
    }
  }

  /** deprecated finalizer - still parseable, still compiles, warns */
  @Deprecated(since = "9", forRemoval = true)
  @SuppressWarnings("removal")
  @Override
  protected void finalize() throws Throwable {
    try {
      log.add("finalized");
    } finally {
      super.finalize();
    }
  }

  /** static factory method */
  public static LifecyclePatterns of(int id) {
    return new LifecyclePatterns(id);
  }

  /** singleton holder idiom - lazy class-loading based initialization */
  public static final class Holder {
    private static final LifecyclePatterns INSTANCE = new LifecyclePatterns(-1);

    private Holder() {
      throw new AssertionError("no instances");
    }

    public static LifecyclePatterns instance() {
      return INSTANCE;
    }
  }
}
