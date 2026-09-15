package com.issuetracker.patterns;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Deliberately awkward-but-legal declarations, aimed at parser / tree-sitter
 * coverage rather than at readability. Everything here compiles on Java 17.
 */
@Component
@SuppressWarnings({"unused", "rawtypes", "unchecked"})
public class ParserTorturePatterns {

  /* ------------------------------------------------------ formatting edge cases */

  /** whole method on one line */
  public int oneLiner(int a,int b){return a+b;}

  /** signature split across many lines with comments interleaved */
  public
  /* modifier comment */
  static
  <
      T /* type param comment */
      >
  List<
      T
      >
  splitSignature(
      // first param
      T a,
      /* second param */ T b
  )
      throws
      IllegalArgumentException {
    return List.of(a, b);
  }

  /** annotation, javadoc and comment stacked before the modifiers */
  @Deprecated
  @SuppressWarnings("all")
  // trailing line comment before the signature
  public final String stackedModifiers() {
    return "stacked";
  }

  /** unicode identifiers and escapes */
  public String ünïcödeMethod(String naïve, String 日本語) {
    String \u0061bc = naïve + 日本語;
    return abc;
  }

  /** identifiers that look like keywords */
  public String recordYieldSealedPermits(String record, String yield, String sealed, String permits) {
    var var = record + yield + sealed + permits;
    return var;
  }

  /* --------------------------------------------------------------- expressions */

  /** text block return */
  public String textBlock() {
    return """
        {
          "kind": "text-block",
          "nested": "quote \\" and backslash \\\\"
        }
        """;
  }

  /** switch expression with arrow, multi-label, yield block and default */
  public String switchExpression(int code) {
    return switch (code) {
      case 1, 2 -> "low";
      case 3 -> {
        String s = "mid";
        yield s.toUpperCase();
      }
      default -> {
        yield code > 100 ? "huge" : "other";
      }
    };
  }

  /** old-style switch statement with fallthrough and labels */
  public String switchStatement(int code) {
    StringBuilder sb = new StringBuilder();
    switch (code) {
      case 1:
        sb.append("one");
        // fallthrough
      case 2:
        sb.append("two");
        break;
      case 3: {
        sb.append("three");
        break;
      }
      default:
        sb.append("default");
    }
    return sb.toString();
  }

  /** labeled loops with break/continue to a label */
  public int labeledLoops() {
    int found = -1;
    outer:
    for (int i = 0; i < 5; i++) {
      inner:
      for (int j = 0; j < 5; j++) {
        if (j == 2) continue inner;
        if (i * j > 6) {
          found = i * j;
          break outer;
        }
      }
    }
    return found;
  }

  /** ternary chains, casts, instanceof pattern, and bit ops in one expression */
  public String denseExpression(Object o) {
    return o instanceof String s && s.length() > 2
        ? (s.hashCode() & 0xFF) > 128 ? "hi-" + s : "lo-" + s
        : o instanceof Integer i
            ? String.valueOf(((i << 2) | 1) >>> 1)
            : (String) (o == null ? "null" : o.toString());
  }

  /** deeply nested generics */
  public Map<String, List<Map<Integer, List<Function<String, List<String>>>>>> deepGenerics() {
    return new HashMap<>();
  }

  /** explicit generic method invocation with a type witness and a nested diamond */
  public List<Map<String, List<Integer>>> typeWitness() {
    return java.util.Collections.<Map<String, List<Integer>>>emptyList();
  }

  /** wildcard soup */
  public static <T extends Comparable<? super T> & java.io.Serializable> int wildcardSoup(
      List<? extends T> src, List<? super T> dst, Map<?, ? extends List<?>> meta) {
    return src.size() + dst.size() + meta.size();
  }

  /** array initializers and nested array types */
  public int[][][] arrays() {
    int[][][] cube = new int[][][] {
        {{1, 2}, {3, 4}},
        {{5, 6}, {7, 8}},
    };
    return cube;
  }

  /** numeric literal zoo */
  public Map<String, Number> literals() {
    Map<String, Number> m = new HashMap<>();
    m.put("hex", 0xCAFE_BABEL);
    m.put("binary", 0b1010_1010);
    m.put("octal", 0777);
    m.put("underscored", 1_000_000);
    m.put("float", 1_2.5e-3f);
    m.put("double", .5d);
    m.put("longHex", 0xFFL);
    return m;
  }

  /** chained builder-ish calls across lines */
  public String chained() {
    return new StringBuilder()
        .append("a")
        .append('b')
        .append(1)
        .append(true)
        .reverse()
        .toString()
        .trim()
        .repeat(2);
  }

  /** anonymous class with an instance initializer (double-brace initialization) */
  public Map<String, String> doubleBrace() {
    return new HashMap<>() {
      {
        put("a", "1");
        put("b", "2");
      }
    };
  }

  /** nested lambdas inside a method reference chain */
  public Function<String, Function<String, String>> nestedLambdas() {
    return a -> b -> ((Function<String, String>) s -> s + "!").apply(a + b);
  }

  /** empty method body, and a method that only throws */
  public void empty() {}

  public String neverReturns() {
    throw new UnsupportedOperationException("by design");
  }

  /** assert statement */
  public boolean withAssert(int value) {
    assert value >= 0 : "value must be non-negative";
    return true;
  }

  /** static method hidden by an instance method of the same name in a nested type */
  public static String hiddenName() {
    return "outer-static";
  }

  public static class Shadower {
    public String hiddenName() {
      return "inner-instance";
    }

    /** shadowed type parameter name reused from an enclosing generic scope */
    public <T> T echo(T t) {
      return t;
    }
  }

  /** generic method whose type variable shadows a class-level name */
  public static class Box<T> {
    private T value;

    public <T> T shadowedTypeParam(T other) {
      return other;
    }

    public T get() {
      return value;
    }

    public Box<T> set(T v) {
      this.value = v;
      return this;
    }
  }

  /** method that declares an unused generic type never referenced in params */
  public <UNUSED> String phantomGeneric() {
    return "phantom";
  }

  /** recursive generic bound (self type / CRTP) */
  public abstract static class SelfBuilder<S extends SelfBuilder<S>> {
    protected String name;

    @SuppressWarnings("unchecked")
    public S name(String n) {
      this.name = n;
      return (S) this;
    }

    public abstract String build();
  }

  public static final class ConcreteBuilder extends SelfBuilder<ConcreteBuilder> {
    @Override
    public String build() {
      return "built:" + name;
    }
  }

  /** vararg of generic arrays plus an annotation on the type use */
  public final <T> int varargOfArrays(T[]... groups) {
    int n = 0;
    for (T[] g : groups) n += g.length;
    return n;
  }

  /** JLS §8.4.1 explicit receiver parameter 'this' — legal in Java 8+ */
  public String explicitReceiver(ParserTorturePatterns this, String suffix) {
    return "receiver:" + suffix;
  }

  /** Intersection type in cast expression */
  public String intersectionCast(Object obj) {
    if (obj instanceof CharSequence && obj instanceof Comparable) {
      Comparable<String> comp = (Comparable<String> & CharSequence) obj;
      return "intersection:" + comp;
    }
    return "non-intersection";
  }

  /** Inner class with explicit enclosing instance receiver and method receiver */
  public class ReceiverInner {
    public ReceiverInner(ParserTorturePatterns ParserTorturePatterns.this) {}
    public String innerReceiver(ReceiverInner this) {
      return "inner-this-receiver";
    }
  }

  /** Generic throws clause: throwing an unhandled checked exception via type erasure */
  public <E extends Throwable> void sneakyThrow(Throwable t) throws E {
    throw (E) t;
  }

  /** fully qualified types everywhere instead of imports */
  public java.util.List<java.lang.String> fullyQualified(java.lang.String s) {
    return java.util.List.of(s);
  }

  public Map<String, Object> summary() {
    Map<String, Object> out = new HashMap<>();
    out.put("oneLiner", oneLiner(1, 2));
    out.put("splitSignature", splitSignature("a", "b"));
    out.put("stackedModifiers", stackedModifiers());
    out.put("explicitReceiver", explicitReceiver("explicit-this"));
    out.put("innerReceiver", new ReceiverInner().innerReceiver());
    out.put("intersectionCast", intersectionCast("string-intersection"));
    out.put("unicode", ünïcödeMethod("naive", "nihongo"));
    out.put("keywordish", recordYieldSealedPermits("r", "y", "s", "p"));
    out.put("textBlock", textBlock());
    out.put("switchExpression", List.of(switchExpression(1), switchExpression(3), switchExpression(999)));
    out.put("switchStatement", switchStatement(1));
    out.put("labeledLoops", labeledLoops());
    out.put("denseExpression", List.of(denseExpression("abc"), denseExpression(9), denseExpression(null)));
    out.put("wildcardSoup", wildcardSoup(List.of("a"), new java.util.ArrayList<Object>(), Map.of()));
    out.put("arrays", arrays().length);
    out.put("literals", literals());
    out.put("chained", chained());
    out.put("doubleBrace", doubleBrace());
    out.put("nestedLambdas", nestedLambdas().apply("x").apply("y"));
    out.put("assert", withAssert(1));
    out.put("hiddenName", hiddenName() + "/" + new Shadower().hiddenName());
    out.put("shadowedTypeParam", new Box<Integer>().shadowedTypeParam("string-not-integer"));
    out.put("phantomGeneric", phantomGeneric());
    out.put("selfBuilder", new ConcreteBuilder().name("cb").build());
    out.put("varargOfArrays", varargOfArrays(new String[] {"a"}, new String[] {"b", "c"}));
    out.put("fullyQualified", fullyQualified("fq"));
    return out;
  }
}
