package com.issuetracker.patterns;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Classic OOP declaration shapes: inheritance, overloading, overriding,
 * abstract / interface / enum / record / annotation members.
 */
@Component
public class OopPatterns {

  // ---------------------------------------------------------- overloading

  public String overloaded() {
    return "no-args";
  }

  public String overloaded(int a) {
    return "int:" + a;
  }

  public String overloaded(long a) {
    return "long:" + a;
  }

  public String overloaded(Integer a) {
    return "boxed:" + a;
  }

  public String overloaded(int a, int b) {
    return "int,int:" + (a + b);
  }

  public String overloaded(String a) {
    return "string:" + a;
  }

  public String overloaded(CharSequence a) {
    return "charseq:" + a;
  }

  public String overloaded(int... values) {
    return "varargs:" + values.length;
  }

  public String overloaded(String first, Object... rest) {
    return "mixed-varargs:" + first + ":" + rest.length;
  }

  @SafeVarargs
  public final <T> String overloadedSafe(T... values) {
    return "safe-varargs:" + values.length;
  }

  /** C-style array return type declaration - legal but rare */
  @SuppressWarnings("all")
  public int oldStyleArray() [] {
    return new int[] {1, 2, 3};
  }

  /** array-of-array parameter with mixed bracket placement */
  public int mixedBrackets(int matrix[][], int[] flat[]) {
    return matrix.length + flat.length;
  }

  /** method with a throws clause listing multiple exceptions */
  public String declaresThrows(boolean fail) throws IllegalStateException, java.io.IOException {
    if (fail) throw new java.io.IOException("io");
    return "no-throw";
  }

  /** strictfp method modifier */
  @SuppressWarnings("strictfp")
  public strictfp double strictMath(double a, double b) {
    return a / b;
  }

  /** synchronized + final + generic + bounded + varargs on one signature */
  public final synchronized <T extends Comparable<? super T>> T maxOf(T first, T... rest) {
    T best = first;
    for (T t : rest) {
      if (t.compareTo(best) > 0) best = t;
    }
    return best;
  }

  // ------------------------------------------------- inheritance hierarchy

  /** abstract base with abstract, concrete, and protected methods */
  public abstract static class Shape {
    protected final String name;

    protected Shape(String name) {
      this.name = name;
    }

    public abstract double area();

    public String describe() {
      return name + "=" + area();
    }

    protected String secretTag() {
      return "shape";
    }

    /** abstract method overloaded with a concrete sibling */
    public abstract double scaled(double factor);

    public double scaled() {
      return scaled(2.0);
    }
  }

  public static class Circle extends Shape {
    private final double r;

    public Circle(double r) {
      super("circle");
      this.r = r;
    }

    @Override
    public double area() {
      return Math.PI * r * r;
    }

    @Override
    public double scaled(double factor) {
      return area() * factor;
    }

    /** covariant return type override */
    public Circle self() {
      return this;
    }

    /** calls the super implementation explicitly */
    @Override
    public String describe() {
      return "C:" + super.describe();
    }
  }

  public static final class UnitCircle extends Circle {
    public UnitCircle() {
      super(1.0);
    }

    @Override
    protected String secretTag() {
      return "unit-" + super.secretTag();
    }
  }

  /** interface with abstract, default, static and private methods */
  public interface Renderer {
    String render(Shape shape);

    default String renderAll(List<Shape> shapes) {
      List<String> parts = new ArrayList<>();
      for (Shape s : shapes) parts.add(render(s));
      return String.join(",", parts);
    }

    static Renderer simple() {
      return prefix("");
    }

    private static Renderer prefix(String p) {
      return shape -> p + shape.describe();
    }

    private String helper() {
      return "renderer";
    }

    default String kind() {
      return helper();
    }
  }

  /** interface extending multiple interfaces */
  public interface Named {
    String name();
  }

  public interface FancyRenderer extends Renderer, Named {
    @Override
    default String name() {
      return "fancy";
    }
  }

  /** sealed interface with permitted implementations (Java 17) */
  public sealed interface Event permits Created, Deleted {}

  public record Created(String id) implements Event {}

  public record Deleted(String id, String reason) implements Event {}

  // ------------------------------------------------------------- records

  /** record with compact constructor, extra constructor, static + instance methods */
  public record Money(String currency, long amountMinor) implements Comparable<Money> {
    public Money {
      if (amountMinor < 0) throw new IllegalArgumentException("negative");
      currency = currency.toUpperCase();
    }

    public Money(long amountMinor) {
      this("usd", amountMinor);
    }

    public static Money zero() {
      return new Money("USD", 0);
    }

    public Money plus(Money other) {
      return new Money(currency, amountMinor + other.amountMinor);
    }

    /** explicit accessor override */
    @Override
    public long amountMinor() {
      return amountMinor;
    }

    @Override
    public int compareTo(Money o) {
      return Long.compare(amountMinor, o.amountMinor);
    }
  }

  /** generic record */
  public record Pair<A, B>(A left, B right) {
    public <C> Pair<A, C> withRight(C c) {
      return new Pair<>(left, c);
    }
  }

  /** local record inside a method (Java 16+) */
  public String localRecord(String key, int value) {
    record Entry(String key, int value) {
      String render() {
        return key + "=" + value;
      }
    }
    return new Entry(key, value).render();
  }

  // --------------------------------------------------------------- enums

  /** enum with fields, constructor, abstract method and constant bodies */
  public enum Op {
    ADD("+") {
      @Override
      public int apply(int a, int b) {
        return a + b;
      }
    },
    SUB("-") {
      @Override
      public int apply(int a, int b) {
        return a - b;
      }
    },
    MUL("*") {
      @Override
      public int apply(int a, int b) {
        return a * b;
      }

      @Override
      public String toString() {
        return "times";
      }
    };

    private final String symbol;

    Op(String symbol) {
      this.symbol = symbol;
    }

    public abstract int apply(int a, int b);

    public String symbol() {
      return symbol;
    }

    public static Op fromSymbol(String s) {
      for (Op op : values()) {
        if (op.symbol.equals(s)) return op;
      }
      throw new IllegalArgumentException(s);
    }
  }

  /** enum implementing an interface, with a static initializer block */
  public enum Level implements Named {
    LOW, MEDIUM, HIGH;

    private static final Map<String, Level> INDEX = new HashMap<>();

    static {
      for (Level l : values()) INDEX.put(l.name().toLowerCase(), l);
    }

    /** cannot override the final Enum.name(); expose an alias instead */
    public String label(/* no params */) {
      return name().toLowerCase();
    }

    public static Level lookup(String key) {
      return INDEX.getOrDefault(key, LOW);
    }
  }

  // ---------------------------------------------------------- annotations

  /** annotation type declaration: methods with defaults and array members */
  public @interface Marker {
    String value() default "";

    int order() default 0;

    Class<?>[] targets() default {};
  }

  @Marker(value = "annotated", order = 3, targets = {String.class, Integer.class})
  public String annotatedMethod() {
    return "annotated";
  }

  // ------------------------------------------------------- inner classes

  /** non-static inner class: needs an outer instance */
  public class Inner {
    public String outerName() {
      return OopPatterns.this.getClass().getSimpleName() + ".Inner";
    }
  }

  /** static nested class */
  public static class Nested {
    public static String staticNestedMethod() {
      return "nested-static";
    }

    public String instanceMethod() {
      return "nested-instance";
    }

    public static class DeeplyNested {
      public String deep() {
        return "deep";
      }
    }
  }

  public Map<String, Object> summary() {
    Map<String, Object> out = new HashMap<>();
    out.put("overloads", List.of(
        overloaded(), overloaded(1), overloaded(1L), overloaded(Integer.valueOf(1)),
        overloaded(1, 2), overloaded("s"), overloaded((CharSequence) new StringBuilder("cs")),
        overloaded(1, 2, 3), overloaded("head", "a", "b"), overloadedSafe("x", "y")));
    out.put("oldStyleArray", oldStyleArray().length);
    out.put("mixedBrackets", mixedBrackets(new int[2][2], new int[3][1]));
    out.put("strictMath", strictMath(10, 4));
    out.put("maxOf", maxOf(3, 9, 4));
    Shape shape = new UnitCircle();
    out.put("describe", shape.describe());
    out.put("scaledDefault", shape.scaled());
    out.put("renderer", Renderer.simple().renderAll(List.of(new Circle(2))));
    out.put("rendererKind", Renderer.simple().kind());
    out.put("money", Money.zero().plus(new Money(250L)));
    out.put("pair", new Pair<>("l", 1).withRight(true));
    out.put("localRecord", localRecord("k", 7));
    out.put("enumOps", List.of(Op.ADD.apply(2, 3), Op.SUB.apply(2, 3), Op.MUL.apply(2, 3)));
    out.put("enumLookup", Level.lookup("high").name());
    out.put("annotated", annotatedMethod());
    out.put("inner", new OopPatterns().new Inner().outerName());
    out.put("nested", Nested.staticNestedMethod() + "/" + new Nested.DeeplyNested().deep());
    out.put("events", List.of(new Created("1"), new Deleted("2", "spam")));
    return out;
  }
}
