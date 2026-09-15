package com.issuetracker.patterns;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Every shape of "a function that is not a plain method".
 */
@Component
public class FunctionalPatterns {

  // ------------------------------------------------- functional interfaces

  /** custom functional interface with a single abstract method */
  @FunctionalInterface
  public interface Transformer<I, O> {
    O apply(I input);

    /** default method on an interface */
    default Transformer<I, O> orElse(O fallback) {
      return in -> {
        O out = apply(in);
        return out == null ? fallback : out;
      };
    }

    /** static method on an interface */
    static <T> Transformer<T, T> identity() {
      return t -> t;
    }

    /** private interface method (Java 9+) */
    private static String tag() {
      return "transformer";
    }

    /** default method that calls the private one */
    default String describe() {
      return tag();
    }
  }

  /** functional interface whose method declares a checked exception */
  @FunctionalInterface
  public interface ThrowingSupplier<T> {
    T get() throws Exception;
  }

  /** generic functional interface with a generic abstract method */
  @FunctionalInterface
  public interface PolyMapper {
    <T> List<T> wrap(T value);
  }

  // ----------------------------------------------------------- lambda forms

  /** zero-arg lambda assigned to a field */
  public final Supplier<String> zeroArgLambda = () -> "zero";

  /** single param without parentheses or type */
  public final Function<String, Integer> implicitParam = s -> s.length();

  /** single param with explicit type and parentheses */
  public final Function<String, Integer> explicitParam = (String s) -> s.length();

  /** single param with var (Java 11+) */
  public final Function<String, String> varParam = (var s) -> s.toUpperCase();

  /** annotated var param */
  public final Predicate<String> annotatedParam = (@Deprecated var s) -> !s.isEmpty();

  /** block-bodied lambda with local state and explicit return */
  public final BiFunction<Integer, Integer, Integer> blockLambda = (a, b) -> {
    int sum = a + b;
    if (sum > 100) {
      return 100;
    }
    return sum;
  };

  /** lambda returning a lambda (curried) */
  public final Function<Integer, Function<Integer, Integer>> curriedAdd = a -> b -> a + b;

  /** triple-curried */
  public final Function<Integer, Function<Integer, Function<Integer, Integer>>> curried3 =
      a -> b -> c -> a * b + c;

  /** primitive specialization lambda */
  public final IntBinaryOperator primitiveLambda = (a, b) -> a ^ b;

  private final List<String> sideEffects = new ArrayList<>();

  /** void-returning lambda with side effect */
  public final Consumer<String> voidLambda = s -> sideEffects.add(s);

  /** unary and binary operator shorthands */
  public final UnaryOperator<String> unary = String::trim;
  public final BinaryOperator<String> binary = (a, b) -> a + b;

  public List<String> sideEffects() {
    return List.copyOf(sideEffects);
  }

  // --------------------------------------------------------- method refs x4

  /** static method reference */
  public final Function<String, Integer> staticRef = Integer::parseInt;

  /** unbound instance method reference on an arbitrary receiver */
  public final Function<String, String> unboundRef = String::toLowerCase;

  /** bound instance method reference on a specific receiver */
  public final Supplier<Integer> boundRef = "bound-receiver"::length;

  /** constructor reference */
  public final Supplier<ArrayList<String>> ctorRef = ArrayList::new;

  /** array constructor reference */
  public final Function<Integer, String[]> arrayCtorRef = String[]::new;

  /** generic method reference with explicit type witness */
  public final Supplier<List<String>> witnessRef = List::<String>of;

  // ------------------------------------------------------- higher order fns

  /** function taking a function */
  public String higherOrder(Function<String, String> fn, String input) {
    return fn.apply(input);
  }

  /** function returning a function */
  public Function<String, String> makePrefixer(String prefix) {
    return value -> prefix + value;
  }

  /** function taking and returning functions (decorator / middleware) */
  public <T, R> Function<T, R> logged(Function<T, R> inner, String name) {
    return t -> {
      sideEffects.add("call:" + name);
      return inner.apply(t);
    };
  }

  /** composition using andThen / compose */
  public String composed(String input) {
    Function<String, String> f = makePrefixer("a-");
    Function<String, String> g = makePrefixer("b-");
    return f.andThen(g).compose(unary).apply(input);
  }

  /** anonymous class implementing the same interface a lambda could */
  public String anonymousImplementation(String input) {
    Transformer<String, String> t = new Transformer<>() {
      @Override
      public String apply(String in) {
        return "anon:" + in;
      }
    };
    return t.apply(input);
  }

  /** anonymous class for a generic-method interface (a lambda cannot do this) */
  public List<String> polyAnonymous(String value) {
    PolyMapper mapper = new PolyMapper() {
      @Override
      public <T> List<T> wrap(T v) {
        return List.of(v);
      }
    };
    return mapper.wrap(value);
  }

  // ----------------------------------------------------- recursion + memo

  /** plain recursion */
  public long fib(int n) {
    return n < 2 ? n : fib(n - 1) + fib(n - 2);
  }

  /** mutual recursion */
  public boolean isEven(int n) {
    return n == 0 || isOdd(n - 1);
  }

  public boolean isOdd(int n) {
    return n != 0 && isEven(n - 1);
  }

  /** tail-shaped recursion with an accumulator and a default-arg overload */
  public long factorial(int n) {
    return factorial(n, 1L);
  }

  private long factorial(int n, long acc) {
    return n <= 1 ? acc : factorial(n - 1, acc * n);
  }

  /** self-referential lambda via an array trick */
  public long lambdaRecursion(int n) {
    @SuppressWarnings("unchecked")
    Function<Integer, Long>[] self = new Function[1];
    self[0] = k -> k < 2 ? (long) k : self[0].apply(k - 1) + self[0].apply(k - 2);
    return self[0].apply(n);
  }

  private final Map<Integer, Long> memo = new HashMap<>();

  /**
   * memoized recursion driven by a lambda.
   * NOTE: recursive computeIfAbsent on a HashMap throws ConcurrentModificationException,
   * so the lookup and the store are kept separate.
   */
  public long memoFib(int n) {
    if (n < 2) return n;
    Long cached = memo.get(n);
    if (cached != null) return cached;
    IntUnaryOperator step = k -> (int) (memoFib(k - 1) + memoFib(k - 2));
    long value = step.applyAsInt(n);
    memo.put(n, value);
    return value;
  }

  // ------------------------------------------------------------- optionals

  /** Optional pipeline: map / filter / flatMap / orElseGet */
  public String optionalPipeline(String raw) {
    return Optional.ofNullable(raw)
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .flatMap(s -> Optional.of(s.toUpperCase()))
        .orElseGet(() -> "EMPTY");
  }

  /** local function emulation: a lambda declared inside a method */
  public int localFunction(int x) {
    UnaryOperator<Integer> doubler = v -> v * 2;
    Function<Integer, Integer> plusOne = v -> v + 1;
    return plusOne.apply(doubler.apply(x));
  }

  /** local class declared inside a method body */
  public String localClass(String input) {
    class Local implements Transformer<String, String> {
      @Override
      public String apply(String in) {
        return "local:" + in;
      }
    }
    return new Local().apply(input);
  }

  /** wrapping a checked-exception supplier into an unchecked one */
  public <T> Supplier<T> unchecked(ThrowingSupplier<T> supplier) {
    return () -> {
      try {
        return supplier.get();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    };
  }

  public Map<String, Object> summary() {
    Map<String, Object> out = new HashMap<>();
    out.put("curried", curried3.apply(2).apply(3).apply(4));
    out.put("composed", composed("  Hello  "));
    out.put("anonymous", anonymousImplementation("x"));
    out.put("memoFib", memoFib(20));
    out.put("lambdaRecursion", lambdaRecursion(15));
    out.put("optional", optionalPipeline("  padded "));
    out.put("localClass", localClass("y"));
    out.put("methodRefs", List.of(staticRef.apply("42"), unboundRef.apply("ABC"), boundRef.get()));
    out.put("describe", Transformer.identity().describe());
    return out;
  }
}
