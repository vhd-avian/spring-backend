package com.issuetracker.patterns;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Java 17 equivalents of C# switch expressions, `is` patterns, records and deconstruction.
 */
@Component
public class PatternMatchingPatterns {

  /* ---------- records (C# records / positional patterns) ---------- */
  public record Point(int x, int y) {
    // compact constructor = validation
    public Point {
      if (x < Integer.MIN_VALUE / 2) throw new IllegalArgumentException("x too small");
    }
    // derived member
    public int manhattan() { return Math.abs(x) + Math.abs(y); }
    // static factory
    public static Point origin() { return new Point(0, 0); }
  }

  public record Product(String name, BigDecimal price) {}

  /* ---------- sealed hierarchy (discriminated union) ---------- */
  public sealed interface Shape permits Circle, Square, Rectangle {}
  public record Circle(double radius) implements Shape {}
  public record Square(double side) implements Shape {}
  public record Rectangle(double w, double h) implements Shape {}

  /** 1. instanceof pattern (C# `is Product p && p.Price > 100`). */
  public boolean isExpensiveProduct(Object obj) {
    return obj instanceof Product p && p.price().compareTo(BigDecimal.valueOf(100)) > 0;
  }

  /** 2. Ternary / conditional expression. */
  public BigDecimal calculateDiscount(Product p) {
    return p.price().compareTo(BigDecimal.valueOf(100)) > 0
        ? p.price().multiply(BigDecimal.valueOf(0.1))
        : BigDecimal.ZERO;
  }

  /** 3. switch expression over a type hierarchy, with guards. */
  public String describeObject(Object obj) {
    if (obj instanceof Integer i) return "int " + i;
    if (obj instanceof String s && s.isBlank()) return "blank string";
    if (obj instanceof String s) return "string " + s;
    if (obj instanceof Point p && p.x() > 10) return "Point " + p.x() + "," + p.y();
    if (obj instanceof Point p) return "Point (small) " + p;
    if (obj instanceof List<?> list && !list.isEmpty())
      return "List " + list.get(0) + "-" + list.get(list.size() - 1);
    return "Unknown";
  }

  /** 4. Exhaustive switch expression over a sealed interface (no default needed at compile time). */
  public double area(Shape shape) {
    // Java 17: sealed types exist, but switch-over-type-patterns is Java 21.
    // The idiomatic 17 form is an instanceof-pattern chain.
    if (shape instanceof Circle c) return Math.PI * c.radius() * c.radius();
    if (shape instanceof Square s) return s.side() * s.side();
    if (shape instanceof Rectangle r) return r.w() * r.h();
    throw new IllegalStateException("unreachable: sealed hierarchy is exhaustive");
  }

  /** 5. Classic switch with arrow labels, multiple constants and yield block. */
  public String classify(int value) {
    return switch (value) {
      case 0 -> "zero";
      case 1, 2, 3 -> "small";
      default -> {
        String bucket = value < 0 ? "negative" : "large";
        yield bucket + "(" + value + ")";
      }
    };
  }

  /** 6. Enum switch (old-style with fallthrough on purpose). */
  public enum Level { LOW, MEDIUM, HIGH }

  @SuppressWarnings("fallthrough")
  public int weight(Level level) {
    int w = 0;
    switch (level) {
      case HIGH:
        w += 2;
        // falls through
      case MEDIUM:
        w += 2;
        // falls through
      case LOW:
        w += 1;
        break;
    }
    return w;
  }

  /** 7. Record accessor destructuring (Java 21 adds `instanceof Rectangle(double w, double h)`). */
  public String nestedRecord(Object o) {
    if (o instanceof Rectangle r) return "rect " + r.w() + "x" + r.h();
    return "not a rect";
  }

  /** 8. Text block + formatted (C# raw string / interpolation). */
  public String textBlock(String who) {
    return """
        {
          "greeting": "hello %s",
          "kind": "text-block"
        }""".formatted(who);
  }
}
