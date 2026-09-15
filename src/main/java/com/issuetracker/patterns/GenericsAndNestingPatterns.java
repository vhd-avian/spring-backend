package com.issuetracker.patterns;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;

/**
 * Generics, nesting, inner/local/anonymous classes, shadowing, wildcards.
 */
@Component
public class GenericsAndNestingPatterns {

  /* ---------- generic outer with a generic (shadowing) inner ---------- */
  public static class Outer<T> {
    private final T value;
    public Outer(T value) { this.value = value; }
    public T value() { return value; }

    /** Inner type parameter deliberately shadows the outer one (C# CS0693 analogue). */
    public class Inner<T> { // NOSONAR - shadowing is the point
      public String ghostMethod(T hidden, Outer<Integer>.Inner<String> specific) {
        return "hidden=" + hidden + ", outerValue=" + value + ", specific=" + (specific != null);
      }
    }

    /** Static nested class: no reference to the enclosing instance. */
    public static class StaticNested<U> {
      public String describe(U u) { return "static-nested:" + u; }
    }
  }

  /* ---------- plain nesting ---------- */
  public static class OuterClass {
    public String outerMethod() { return "outer"; }

    public static class NestedClass {
      public String nestedMethod() { return "nested"; }

      /** Local (method-scoped) class + local function equivalent. */
      public String methodWithLocalFunction() {
        class Local {
          String run() { return "local-class"; }
        }
        Function<String, String> localLambda = s -> s + "+lambda"; // the "local function"
        return localLambda.apply(new Local().run());
      }
    }

    /** Non-static inner class needs an enclosing instance. */
    public class InnerNonStatic {
      public String combined() { return outerMethod() + "/inner"; }
    }
  }

  /* ---------- generic methods ---------- */

  /** Bounded type parameter. */
  public <T extends Number> double sum(List<T> numbers) {
    double total = 0;
    for (T n : numbers) total += n.doubleValue();
    return total;
  }

  /** Multiple bounds. */
  public <T extends Comparable<T> & java.io.Serializable> T pickMax(T a, T b) {
    return a.compareTo(b) >= 0 ? a : b;
  }

  /** Covariant wildcard (IEnumerable<out T>). */
  public double sumOfAny(Collection<? extends Number> items) {
    double total = 0;
    for (Number n : items) total += n.doubleValue();
    return total;
  }

  /** Contravariant wildcard (Action<in T>). */
  public void fill(Collection<? super Integer> sink, int count) {
    for (int i = 0; i < count; i++) sink.add(i);
  }

  /** Recursive generic bound (self type / fluent builder). */
  public abstract static class SelfBuilder<S extends SelfBuilder<S>> {
    protected final StringBuilder buffer = new StringBuilder();
    @SuppressWarnings("unchecked")
    public S append(String s) { buffer.append(s); return (S) this; }
    public String build() { return buffer.toString(); }
  }

  public static class MessageBuilder extends SelfBuilder<MessageBuilder> {
    public MessageBuilder exclaim() { return append("!"); }
  }

  /** Generic pair with static factory and type inference. */
  public record Pair<A, B>(A first, B second) {
    public static <A, B> Pair<A, B> of(A a, B b) { return new Pair<>(a, b); }
    public <C> Pair<A, C> withSecond(C c) { return new Pair<>(first, c); }
  }

  /** Anonymous class vs lambda vs method reference. */
  public List<String> anonymousVsLambda() {
    Comparator<String> anonymous = new Comparator<>() {
      @Override public int compare(String a, String b) { return a.compareTo(b); }
    };
    Comparator<String> lambda = (a, b) -> a.compareTo(b);
    Comparator<String> methodRef = String::compareTo;
    List<String> data = new ArrayList<>(List.of("c", "a", "b"));
    data.sort(anonymous.thenComparing(lambda).thenComparing(methodRef));
    return data;
  }

  /** Type erasure demo: both lists share one runtime class. */
  public String erasure() {
    List<String> strings = new ArrayList<>();
    List<Integer> ints = new ArrayList<>();
    return strings.getClass() == ints.getClass() ? "erased (same runtime class)" : "reified";
  }

  public Map<String, Object> runAll() {
    Outer<String> outer = new Outer<>("outer-value");
    Outer<String>.Inner<Integer> inner = outer.new Inner<>();
    OuterClass oc = new OuterClass();

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("shadowedInner", inner.ghostMethod(42, null));
    out.put("staticNested", new Outer.StaticNested<String>().describe("u"));
    out.put("nested", new OuterClass.NestedClass().nestedMethod());
    out.put("localFunction", new OuterClass.NestedClass().methodWithLocalFunction());
    out.put("innerNonStatic", oc.new InnerNonStatic().combined());
    out.put("sum", sum(List.of(1, 2, 3.5)));
    out.put("pickMax", pickMax("apple", "banana"));
    out.put("selfBuilder", new MessageBuilder().append("hi").exclaim().build());
    out.put("pair", Pair.of("k", 1).withSecond(true).toString());
    out.put("sorted", anonymousVsLambda());
    out.put("erasure", erasure());
    return out;
  }
}
