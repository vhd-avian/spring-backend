package com.issuetracker.patterns;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Public, unauthenticated showcase of Java coding patterns.
 * Each endpoint is a thin controller method that calls a distinct internal function.
 *
 * Base path: /api/v1/patterns
 */
@RestController
@RequestMapping(value = "/api/v1/patterns", produces = MediaType.APPLICATION_JSON_VALUE)
public class PatternsController {

  private final AsyncPatterns async;
  private final StreamAndIteratorPatterns streams;
  private final PatternMatchingPatterns matching;
  private final GenericsAndNestingPatterns generics;
  private final LifecyclePatterns lifecycle;
  private final FunctionalPatterns functional;
  private final ConcurrencyPatterns concurrency;
  private final OopPatterns oop;
  private final ParserTorturePatterns torture;

  public PatternsController(AsyncPatterns async,
                            StreamAndIteratorPatterns streams,
                            PatternMatchingPatterns matching,
                            GenericsAndNestingPatterns generics,
                            LifecyclePatterns lifecycle,
                            FunctionalPatterns functional,
                            ConcurrencyPatterns concurrency,
                            OopPatterns oop,
                            ParserTorturePatterns torture) {
    this.async = async;
    this.streams = streams;
    this.matching = matching;
    this.generics = generics;
    this.lifecycle = lifecycle;
    this.functional = functional;
    this.concurrency = concurrency;
    this.oop = oop;
    this.torture = torture;
  }

  /* ------------------------------------------------------------------ *
   * Index
   * ------------------------------------------------------------------ */
  @GetMapping
  public Map<String, Object> index() {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("description", "Unauthenticated catalogue of Java method/execution patterns");
    out.put("groups", List.of("sync", "async", "streams", "matching", "generics",
        "lifecycle", "functional", "concurrency", "oop", "torture", "all"));
    out.put("endpoints", List.of(
        "GET /api/v1/patterns/sync",
        "GET /api/v1/patterns/sync/generic?a=1&b=2",
        "GET /api/v1/patterns/async",
        "GET /api/v1/patterns/async/fallback?fail=true",
        "GET /api/v1/patterns/async/callable",
        "GET /api/v1/patterns/async/deferred",
        "POST /api/v1/patterns/async/fire-and-forget?message=hi",
        "GET /api/v1/patterns/streams",
        "GET /api/v1/patterns/matching?value=42",
        "GET /api/v1/patterns/generics",
        "GET /api/v1/patterns/lifecycle",
        "GET /api/v1/patterns/lifecycle/guarded?mode=ok",
        "GET /api/v1/patterns/functional",
        "GET /api/v1/patterns/functional/higher-order?input=abc",
        "GET /api/v1/patterns/concurrency",
        "GET /api/v1/patterns/oop",
        "GET /api/v1/patterns/oop/overloads?value=7",
        "GET /api/v1/patterns/torture",
        "GET /api/v1/patterns/torture/switch?code=2",
        "GET /api/v1/patterns/all"));
    return out;
  }

  /* ------------------------------------------------------------------ *
   * Synchronous method flavours
   * ------------------------------------------------------------------ */
  @GetMapping("/sync")
  public Map<String, Object> sync() {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("standardSync", async.standardSync());
    out.put("syncDelegating", async.syncDelegating());
    out.put("staticHelper", AsyncPatterns.staticHelper());
    out.put("finalMethod", async.finalMethod());
    out.put("synchronizedMethod", async.synchronizedMethod());
    out.put("varargs", async.varargs("a", "b", "c"));
    return out;
  }

  /** Generic method with a type parameter bound. */
  @GetMapping("/sync/generic")
  public Map<String, Object> genericMethod(@RequestParam(defaultValue = "1") int a,
                                           @RequestParam(defaultValue = "2") int b) {
    return Map.of("maxOf", async.maxOf(a, b));
  }

  /* ------------------------------------------------------------------ *
   * Asynchronous flavours
   * ------------------------------------------------------------------ */
  @GetMapping("/async")
  public CompletableFuture<Map<String, Object>> asyncAll() {
    CompletableFuture<String> standard = async.standardAsync();
    CompletableFuture<String> explicit = async.withExplicitExecutor();
    CompletableFuture<String> composed = async.complexAsync();
    CompletableFuture<List<String>> all = async.whenAll();
    CompletableFuture<Object> any = async.whenAny();
    CompletableFuture<String> timeout = async.asyncWithTimeout();
    CompletableFuture<String> spring = async.springAsync();
    CompletableFuture<String> delayed = async.delayed();

    return CompletableFuture
        .allOf(standard, explicit, composed, all, any, timeout, spring, delayed)
        .thenApply(ignored -> {
          Map<String, Object> out = new LinkedHashMap<>();
          out.put("standardAsync", standard.join());
          out.put("withExplicitExecutor", explicit.join());
          out.put("complexAsync", composed.join());
          out.put("whenAll", all.join());
          out.put("whenAny", any.join());
          out.put("asyncWithTimeout", timeout.join());
          out.put("springAsync", spring.join());
          out.put("delayed", delayed.join());
          out.put("perTaskExecutor", async.perTaskExecutor());
          return out;
        });
  }

  /** Async with error recovery (exceptionally / handle). */
  @GetMapping("/async/fallback")
  public CompletableFuture<Map<String, Object>> asyncFallback(
      @RequestParam(defaultValue = "true") boolean fail) {
    return async.asyncWithFallback(fail).thenApply(v -> Map.of("result", (Object) v, "failed", fail));
  }

  /** Callable — the servlet container awaits the value on another thread. */
  @GetMapping("/async/callable")
  public Callable<String> callable() {
    return async.callableEndpointBody();
  }

  /** DeferredResult — completed later by a different thread. */
  @GetMapping("/async/deferred")
  public DeferredResult<String> deferred() {
    return async.deferred();
  }

  /** Fire-and-forget: returns immediately, work continues in the background. */
  @PostMapping("/async/fire-and-forget")
  public Map<String, Object> fireAndForget(@RequestParam(defaultValue = "hello") String message) {
    async.fireAndForget(message);
    return Map.of("accepted", true, "message", message);
  }

  /* ------------------------------------------------------------------ *
   * Streams / iterators (IEnumerable, yield, IAsyncEnumerable analogues)
   * ------------------------------------------------------------------ */
  @GetMapping("/streams")
  public Map<String, Object> streamPatterns() throws InterruptedException {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("eagerSquares", streams.eagerSquares());
    out.put("lazySquares", streams.lazySquares().limit(5).collect(Collectors.toList()));
    out.put("fibonacci", streams.fibonacci(10));
    List<Integer> handWritten = new ArrayList<>();
    streams.handWrittenIterator(5).forEach(handWritten::add);
    out.put("handWrittenIterator", handWritten);
    out.put("customSpliterator", streams.customSpliterator());
    out.put("supplierGenerator", streams.supplierGenerator());
    out.put("asyncStream", streams.asyncStream());
    out.put("collectors", streams.collectors());
    out.put("parallelStream", streams.parallelStream());
    out.put("pipeline", streams.pipeline());
    return out;
  }

  /* ------------------------------------------------------------------ *
   * Pattern matching / records / sealed types
   * ------------------------------------------------------------------ */
  @GetMapping("/matching")
  public Map<String, Object> matchingPatterns(@RequestParam(defaultValue = "42") int value) {
    PatternMatchingPatterns.Product product =
        new PatternMatchingPatterns.Product("Keyboard", new BigDecimal("150.00"));

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("isExpensiveProduct", matching.isExpensiveProduct(product));
    out.put("calculateDiscount", matching.calculateDiscount(product));
    out.put("describeObject", matching.describeObject(value));
    out.put("areaCircle", matching.area(new PatternMatchingPatterns.Circle(2)));
    out.put("areaSquare", matching.area(new PatternMatchingPatterns.Square(3)));
    out.put("areaRectangle", matching.area(new PatternMatchingPatterns.Rectangle(2, 5)));
    out.put("classify", matching.classify(value));
    out.put("weightHigh", matching.weight(PatternMatchingPatterns.Level.HIGH));
    out.put("nestedRecord", matching.nestedRecord(new PatternMatchingPatterns.Rectangle(1, 2)));
    out.put("record", PatternMatchingPatterns.Point.origin().manhattan());
    out.put("textBlock", matching.textBlock("world"));
    return out;
  }

  /* ------------------------------------------------------------------ *
   * Generics, nesting, shadowing, erasure
   * ------------------------------------------------------------------ */
  @GetMapping("/generics")
  public Map<String, Object> genericsPatterns() {
    Map<String, Object> out = new LinkedHashMap<>(generics.runAll());
    out.put("sumOfAny", generics.sumOfAny(List.of(1, 2L, 3.5)));
    List<Object> sink = new ArrayList<>();
    generics.fill(sink, 3);
    out.put("contravariantFill", sink);
    return out;
  }

  /* ------------------------------------------------------------------ *
   * Lifecycle: initializers, constructors, try-with-resources, cleanup
   * ------------------------------------------------------------------ */
  @GetMapping("/lifecycle")
  public Map<String, Object> lifecyclePatterns() throws Exception {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("lifecycleLog", lifecycle.lifecycleLog());
    out.put("id", lifecycle.id());
    out.put("tryWithResources", lifecycle.useResource());
    out.put("multipleResources", lifecycle.useResources());
    out.put("staticFactory", LifecyclePatterns.of(99).id());
    out.put("singletonHolder", LifecyclePatterns.Holder.instance().id());
    return out;
  }

  /** Multi-catch / finally behaviour driven by the requested mode. */
  @GetMapping("/lifecycle/guarded")
  public Map<String, Object> lifecycleGuarded(@RequestParam(defaultValue = "ok") String mode) {
    return Map.of("mode", mode, "result", lifecycle.guardedCall(mode));
  }

  /* ------------------------------------------------------------------ *
   * Functional: lambdas, method references, currying, recursion
   * ------------------------------------------------------------------ */
  @GetMapping("/functional")
  public Map<String, Object> functionalPatterns() {
    Map<String, Object> out = new LinkedHashMap<>(functional.summary());
    out.put("zeroArgLambda", functional.zeroArgLambda.get());
    out.put("implicitParam", functional.implicitParam.apply("four"));
    out.put("explicitParam", functional.explicitParam.apply("five!"));
    out.put("varParam", functional.varParam.apply("upper"));
    out.put("blockLambda", functional.blockLambda.apply(6, 7));
    out.put("primitiveLambda", functional.primitiveLambda.applyAsInt(6, 3));
    out.put("curriedAdd", functional.curriedAdd.apply(2).apply(3));
    out.put("fib", functional.fib(12));
    out.put("factorial", functional.factorial(10));
    out.put("isEven", functional.isEven(10));
    out.put("localFunction", functional.localFunction(4));
    out.put("polyAnonymous", functional.polyAnonymous("poly"));
    return out;
  }

  /** Higher-order function taking a function parameter. */
  @GetMapping("/functional/higher-order")
  public Map<String, Object> functionalHigherOrder(@RequestParam(defaultValue = "abc") String input) {
    return Map.of(
        "higherOrder", functional.higherOrder(String::toUpperCase, input),
        "prefixed", functional.makePrefixer("pre-").apply(input),
        "logged", functional.logged(String::length, "len").apply(input));
  }

  /* ------------------------------------------------------------------ *
   * Concurrency: locks, threads, pools, fork/join
   * ------------------------------------------------------------------ */
  @GetMapping("/concurrency")
  public Map<String, Object> concurrencyPatterns() throws Exception {
    Map<String, Object> out = new LinkedHashMap<>(concurrency.summary());
    concurrency.lambdaRunnable.run();
    concurrency.anonymousRunnable.run();
    out.put("runnablesExecuted", true);
    return out;
  }

  /* ------------------------------------------------------------------ *
   * OOP: overloads, inheritance, interfaces, enums, records, annotations
   * ------------------------------------------------------------------ */
  @GetMapping("/oop")
  public Map<String, Object> oopPatterns() {
    return oop.summary();
  }

  /** Overload resolution across primitives, boxes, varargs and CharSequence. */
  @GetMapping("/oop/overloads")
  public Map<String, Object> oopOverloads(@RequestParam(defaultValue = "7") int value) {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("noArgs", oop.overloaded());
    out.put("intArg", oop.overloaded(value));
    out.put("longArg", oop.overloaded((long) value));
    out.put("boxedArg", oop.overloaded(Integer.valueOf(value)));
    out.put("twoInts", oop.overloaded(value, value));
    out.put("stringArg", oop.overloaded(String.valueOf(value)));
    out.put("charSequenceArg", oop.overloaded(new StringBuilder("cs")));
    out.put("varargs", oop.overloaded(1, 2, 3));
    out.put("stringPlusObjects", oop.overloaded("head", 1, 2));
    out.put("genericVarargs", oop.overloadedSafe("a", "b"));
    out.put("maxOf", oop.maxOf(1, 9, 5));
    return out;
  }

  /* ------------------------------------------------------------------ *
   * Parser torture: unusual but legal syntax
   * ------------------------------------------------------------------ */
  @GetMapping("/torture")
  public Map<String, Object> torturePatterns() {
    Map<String, Object> out = new LinkedHashMap<>(torture.summary());
    out.put("oneLiner", torture.oneLiner(2, 3));
    out.put("stackedModifiers", torture.stackedModifiers());
    out.put("unicode", torture.ünïcödeMethod("naive", "japanese"));
    out.put("contextualKeywords",
        torture.recordYieldSealedPermits("record", "yield", "sealed", "permits"));
    out.put("textBlock", torture.textBlock());
    out.put("labeledLoops", torture.labeledLoops());
    out.put("chained", torture.chained());
    out.put("doubleBrace", torture.doubleBrace());
    out.put("nestedLambdas", torture.nestedLambdas().apply("a").apply("b"));
    out.put("fullyQualified", torture.fullyQualified("fq"));
    return out;
  }

  /** Switch expression vs. classic switch statement. */
  @GetMapping("/torture/switch")
  public Map<String, Object> tortureSwitch(@RequestParam(defaultValue = "2") int code) {
    return Map.of(
        "switchExpression", torture.switchExpression(code),
        "switchStatement", torture.switchStatement(code));
  }

  /* ------------------------------------------------------------------ *
   * Everything synchronous, in one payload
   * ------------------------------------------------------------------ */
  @GetMapping("/all")
  public Map<String, Object> all() throws Exception {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("sync", sync());
    out.put("streams", streamPatterns());
    out.put("matching", matchingPatterns(42));
    out.put("generics", genericsPatterns());
    out.put("lifecycle", lifecyclePatterns());
    out.put("functional", functionalPatterns());
    out.put("concurrency", concurrencyPatterns());
    out.put("oop", oopPatterns());
    out.put("torture", torturePatterns());
    return out;
  }
}
