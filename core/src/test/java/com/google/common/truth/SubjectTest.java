/*
 * Copyright (c) 2011 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.common.truth;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Iterators;
import com.google.common.testing.NullPointerTester;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Tests for generic Subject behaviour.
 *
 * @author David Saff
 * @author Christian Gruber
 */
@RunWith(JUnit4.class)
public class SubjectTest {

  @Test public void nullPointerTester() {
    NullPointerTester npTester = new NullPointerTester();

    // TODO(kak): Automatically generate this list with reflection,
    // or maybe use AbstractPackageSanityTests?
    npTester.testAllPublicInstanceMethods(assertThat(false));
    npTester.testAllPublicInstanceMethods(assertThat(String.class));
    npTester.testAllPublicInstanceMethods(assertThat((Comparable) "hello"));
    npTester.testAllPublicInstanceMethods(assertThat(2d));
    npTester.testAllPublicInstanceMethods(assertThat(1));
    npTester.testAllPublicInstanceMethods(assertThat(ImmutableList.of()));
    npTester.testAllPublicInstanceMethods(assertThat(ImmutableListMultimap.of()));
    npTester.testAllPublicInstanceMethods(assertThat(1L));
    npTester.testAllPublicInstanceMethods(assertThat(ImmutableMap.of()));
    npTester.testAllPublicInstanceMethods(assertThat(ImmutableMultimap.of()));
    npTester.testAllPublicInstanceMethods(assertThat(ImmutableMultiset.of()));
    npTester.testAllPublicInstanceMethods(assertThat(Optional.absent()));
    npTester.testAllPublicInstanceMethods(assertThat(ImmutableSetMultimap.of()));
    npTester.testAllPublicInstanceMethods(assertThat("hello"));
    npTester.testAllPublicInstanceMethods(assertThat(new Object()));
    npTester.testAllPublicInstanceMethods(assertThat(ImmutableTable.of()));
    npTester.testAllPublicInstanceMethods(assertThat(BigDecimal.TEN));
  }

  @Test public void allAssertThatOverloadsAcceptNull() throws Exception {
    NullPointerTester npTester = new NullPointerTester();
    for (Method method : Truth.class.getDeclaredMethods()) {
      if (Modifier.isPublic(method.getModifiers())
          && method.getName().equals("assertThat")
          && method.getParameterTypes().length == 1) {
        Object actual = null;
        Subject<?, ?> subject = (Subject<?, ?>) method.invoke(Truth.class, actual);

        subject.isNull();
        try {
          subject.isNotNull(); // should throw
          fail("assertThat(null).isNotNull() should throw an exception!");
        } catch (AssertionError expected) {
          assertThat(expected).hasMessage("Not true that the subject is a non-null reference");
        }

        subject.isSameAs(null);
        subject.isNotSameAs(new Object());

        subject.isNotIn(ImmutableList.<Object>of());
        subject.isNoneOf(new Object(), new Object());

        // This is a hack...but we have to skip DoubleSubject (requires a tolerance)
        // and array-based subjects (they require a primitive array for the actual value).
        if (subject instanceof DoubleSubject || subject instanceof AbstractArraySubject) {
          continue;
        }

        // check all public assertion methods for correct null handling
        npTester.testAllPublicInstanceMethods(subject);

        subject.isNotEqualTo(new Object());
        subject.isEqualTo(null);
        try {
          subject.isEqualTo(new Object()); // should throw
          fail("assertThat(null).isEqualTo(<non-null>) should throw an exception!");
        } catch (AssertionError expected) {
          assertThat(expected.getMessage()).contains("Not true that ");
          assertThat(expected.getMessage()).contains(" is equal to ");
        }
      }
    }
  }

  private static final Object OBJECT_1 = new Object() {
    @Override
    public String toString() {
      return "Object 1";
    }
  };
  private static final Object OBJECT_2 = new Object() {
    @Override
    public String toString() {
      return "Object 2";
    }
  };

  @Test public void toStringsAreIdentical() {
    IntWrapper wrapper = new IntWrapper();
    wrapper.wrapped = 5;
    try {
      assertThat(5).isEqualTo(wrapper);
      fail("Should have thrown.");
    } catch (AssertionError expected) {
      assertThat(expected).hasMessage(
          "Not true that <5> (java.lang.Integer) "
          + "is equal to <5> (com.google.common.truth.SubjectTest$IntWrapper)");
    }
  }

  private static class IntWrapper {
    int wrapped;
    @Override
    public String toString() {
      return Integer.toString(wrapped);
    }
  }

  @Test public void isSameAsWithNulls() {
    Object o = null;
    assertThat(o).isSameAs(null);
  }

  @Test public void isSameAsFailureWithNulls() {
    Object o = null;
    try {
      assertThat(o).isSameAs("a");
      fail("Should have thrown.");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Not true that <null> is the same instance as <a>");
    }
  }

  @Test public void isSameAsWithSameObject() {
    Object a = new Object();
    Object b = a;
    assertThat(a).isSameAs(b);
  }

  @Test public void isSameAsFailureWithObjects() {
    Object a = OBJECT_1;
    Object b = OBJECT_2;
    try {
      assertThat(a).isSameAs(b);
      fail("Should have thrown.");
    } catch (AssertionError e) {
      assertThat(e).hasMessage(
          "Not true that <Object 1> is the same instance as <Object 2>");
    }
  }

  @Test public void isSameAsFailureWithComparableObjects() {
    Object a = "ab";
    Object b = new StringBuilder().append("a").append("b").toString();
    try {
      assertThat(a).isSameAs(b);
      fail("Should have thrown.");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Not true that <ab> is the same instance as <ab>");
    }
  }

  @Test public void isSameAsFailureWithDifferentTypesAndSameToString() {
    Object a = "true";
    Object b = true;
    try {
      assertThat(a).isSameAs(b);
      fail("Should have thrown.");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Not true that <true> (java.lang.String) is the same"
          + " instance as <true> (java.lang.Boolean)");
    }
  }

  @Test public void isNotSameAsWithNulls() {
    Object o = null;
    assertThat(o).isNotSameAs("a");
  }

  @Test public void isNotSameAsFailureWithNulls() {
    Object o = null;
    try {
      assertThat(o).isNotSameAs(null);
      fail("Should have thrown.");
    } catch (AssertionError e) {
      assertThat(e).hasMessage(
          "Not true that <null> is not the same instance as <null>");
    }
  }

  @Test public void isNotSameAsWithObjects() {
    Object a = new Object();
    Object b = new Object();
    assertThat(a).isNotSameAs(b);
  }

  @Test public void isNotSameAsFailureWithSameObject() {
    Object a = OBJECT_1;
    Object b = a;
    try {
      assertThat(a).isNotSameAs(b);
      fail("Should have thrown.");
    } catch (AssertionError e) {
      assertThat(e).hasMessage(
          "Not true that <Object 1> is not the same instance as <Object 1>");
    }
  }

  @Test public void isNotSameAsWithComparableObjects() {
    Object a = "ab";
    Object b = new StringBuilder().append("a").append("b").toString();
    assertThat(a).isNotSameAs(b);
  }

  @Test public void isNotSameAsWithDifferentTypesAndSameToString() {
    Object a = "true";
    Object b = true;
    assertThat(a).isNotSameAs(b);
  }

  @Test public void isNull() {
    Object o = null;
    assertThat(o).isNull();
  }

  @Test public void isNullFail() {
    Object o = new Object();
    try {
      assertThat(o).isNull();
      fail("Should have thrown.");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Not true that <" + o.toString() + "> is null");
    }
  }

  @Test public void stringIsNullFail() {
    try {
      assertThat("foo").isNull();
      fail("Should have thrown.");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Not true that <\"foo\"> is null");
    }
  }

  @Test public void isNotNull() {
    Object o = new Object();
    assertThat(o).isNotNull();
  }

  @Test public void isNotNullFail() {
    Object o = null;
    try {
      assertThat(o).isNotNull();
      fail("Should have thrown.");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Not true that the subject is a non-null reference");
    }
  }

  @Test public void isEqualToWithNulls() {
    Object o = null;
    assertThat(o).isEqualTo(null);
  }

  @Test public void isEqualToFailureWithNulls() {
    Object o = null;
    try {
      assertThat(o).isEqualTo("a");
      fail("Should have thrown.");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Not true that <null> is equal to <a>");
    }
  }

  @Test public void isEqualToWithSameObject() {
    Object a = new Object();
    Object b = a;
    assertThat(a).isEqualTo(b);
  }

  @Test public void isEqualToFailureWithObjects() {
    Object a = OBJECT_1;
    Object b = OBJECT_2;
    try {
      assertThat(a).isEqualTo(b);
      fail("Should have thrown.");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Not true that <Object 1> is equal to <Object 2>");
    }
  }

  @Test public void isEqualToWithComparableObjects() {
    Object a = "ab";
    Object b = new StringBuilder().append("a").append("b").toString();
    assertThat(a).isEqualTo(b);
  }

  @Test public void isEqualToFailureWithComparableObjects() {
    Object a = "ab";
    Object b = new StringBuilder().append("a").append("a").toString();
    try {
      assertThat(a).isEqualTo(b);
      fail("Should have thrown.");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Not true that <ab> is equal to <aa>");
    }
  }

  @Test public void isEqualToFailureWithDifferentTypesAndSameToString() {
    Object a = "true";
    Object b = true;
    try {
      assertThat(a).isEqualTo(b);
      fail("Should have thrown.");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Not true that <true> (java.lang.String) is equal to"
          + " <true> (java.lang.Boolean)");
    }
  }

  @Test public void isNotEqualToWithNulls() {
    Object o = null;
    assertThat(o).isNotEqualTo("a");
  }

  @Test public void isNotEqualToFailureWithNulls() {
    Object o = null;
    try {
      assertThat(o).isNotEqualTo(null);
      fail("Should have thrown.");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Not true that <null> is not equal to <null>");
    }
  }

  @Test public void isNotEqualToWithObjects() {
    Object a = new Object();
    Object b = new Object();
    assertThat(a).isNotEqualTo(b);
  }

  @Test public void isNotEqualToFailureWithObjects() {
    Object o = null;
    try {
      assertThat(o).isNotEqualTo(null);
      fail("Should have thrown.");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Not true that <null> is not equal to <null>");
    }
  }

  @Test public void isNotEqualToFailureWithSameObject() {
    Object a = OBJECT_1;
    Object b = a;
    try {
      assertThat(a).isNotEqualTo(b);
      fail("Should have thrown.");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Not true that <Object 1> is not equal to <Object 1>");
    }
  }

  @Test public void isNotEqualToWithComparableObjects() {
    Object a = "ab";
    Object b = new StringBuilder().append("a").append("a").toString();
    assertThat(a).isNotEqualTo(b);
  }

  @Test public void isNotEqualToFailureWithComparableObjects() {
    Object a = "ab";
    Object b = new StringBuilder().append("a").append("b").toString();
    try {
      assertThat(a).isNotEqualTo(b);
      fail("Should have thrown.");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Not true that <ab> is not equal to <ab>");
    }
  }

  @Test public void isNotEqualToWithDifferentTypesAndSameToString() {
    Object a = "true";
    Object b = true;
    assertThat(a).isNotEqualTo(b);
  }

  @Test public void isInstanceOf() {
    assertThat("a").isInstanceOf(String.class);
  }

  @Test public void isInstanceOfFail() {
    try {
      assertThat(4.5).isInstanceOf(Long.class);
      fail("Should have thrown.");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Not true that <4.5> is an instance of <java.lang.Long>."
          + " It is an instance of <java.lang.Double>");
    }
  }

  @Test public void isNotInstanceOf() {
    assertThat("a").isNotInstanceOf(Long.class);
  }

  @Test public void isNotInstanceOfFail() {
    try {
      assertThat(5).isNotInstanceOf(Number.class);
      fail("Should have thrown.");
    } catch (AssertionError e) {
      assertThat(e).hasMessage(
          "<5> expected not to be an instance of java.lang.Number, but was.");
    }
  }

  @Test public void isIn() {
    assertThat("b").isIn(oneShotIterable("a", "b", "c"));
  }

  @Test public void isInJustTwo() {
    assertThat("b").isIn(oneShotIterable("a", "b"));
  }

  @Test public void isInFailure() {
    try {
      assertThat("x").isIn(oneShotIterable("a", "b", "c"));
      fail("Should have thrown.");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Not true that <\"x\"> is equal to any element in <[a, b, c]>");
    }
  }

  @Test public void isInNullInListWithNull() {
    assertThat((String) null).isIn(oneShotIterable("a", "b", (String) null));
  }

  @Test public void isInNonnullInListWithNull() {
    assertThat("b").isIn(oneShotIterable("a", "b", (String) null));
  }

  @Test public void isInNullFailure() {
    try {
      assertThat((String) null).isIn(oneShotIterable("a", "b", "c"));
      fail("Should have thrown.");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Not true that <\"null\"> is equal to any element in <[a, b, c]>");
    }
  }

  @Test public void isInEmptyFailure() {
    try {
      assertThat("b").isIn(ImmutableList.<String>of());
      fail("Should have thrown.");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Not true that <\"b\"> is equal to any element in <[]>");
    }
  }

  @Test public void isAnyOf() {
    assertThat("b").isAnyOf("a", "b", "c");
  }

  @Test public void isAnyOfJustTwo() {
    assertThat("b").isAnyOf("a", "b");
  }

  @Test public void isAnyOfFailure() {
    try {
      assertThat("x").isAnyOf("a", "b", "c");
      fail("Should have thrown.");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Not true that <\"x\"> is equal to any of <[a, b, c]>");
    }
  }

  @Test public void isAnyOfNullInListWithNull() {
    assertThat((String) null).isAnyOf("a", "b", (String) null);
  }

  @Test public void isAnyOfNonnullInListWithNull() {
    assertThat("b").isAnyOf("a", "b", (String) null);
  }

  @Test public void isAnyOfNullFailure() {
    try {
      assertThat((String) null).isAnyOf("a", "b", "c");
      fail("Should have thrown.");
    } catch (AssertionError e) {
      assertThat(e).hasMessage("Not true that <\"null\"> is equal to any of <[a, b, c]>");
    }
  }

  @Test public void isNotIn() {
    assertThat("x").isNotIn(oneShotIterable("a", "b", "c"));
  }

  @Test public void isNotInFailure() {
    try {
      assertThat("b").isNotIn(oneShotIterable("a", "b", "c"));
      fail("Should have thrown.");
    } catch (AssertionError e) {
      assertThat(e)
          .hasMessage("Not true that <\"b\"> is not in [a, b, c]. It was found at index 1");
    }
  }

  @Test public void isNotInNull() {
    assertThat((String) null).isNotIn(oneShotIterable("a", "b", "c"));
  }

  @Test public void isNotInNullFailure() {
    try {
      assertThat((String) null).isNotIn(oneShotIterable("a", "b", (String) null));
      fail("Should have thrown.");
    } catch (AssertionError e) {
      assertThat(e)
          .hasMessage("Not true that <\"null\"> is not in [a, b, null]. It was found at index 2");
    }
  }

  @Test public void isNotInEmpty() {
    assertThat("b").isNotIn(ImmutableList.<String>of());
  }

  @Test public void isNoneOf() {
    assertThat("x").isNoneOf("a", "b", "c");
  }

  @Test public void isNoneOfFailure() {
    try {
      assertThat("b").isNoneOf("a", "b", "c");
      fail("Should have thrown.");
    } catch (AssertionError e) {
      assertThat(e)
          .hasMessage("Not true that <\"b\"> is not in [a, b, c]. It was found at index 1");
    }
  }

  @Test public void isNoneOfNull() {
    assertThat((String) null).isNoneOf("a", "b", "c");
  }

  @Test public void isNoneOfNullFailure() {
    try {
      assertThat((String) null).isNoneOf("a", "b", (String) null);
      fail("Should have thrown.");
    } catch (AssertionError e) {
      assertThat(e)
          .hasMessage("Not true that <\"null\"> is not in [a, b, null]. It was found at index 2");
    }
  }

  @Test public void throwableHasInitedCause() {
    NullPointerException cause = new NullPointerException();
    String msg = "foo";
    try {
      Truth.THROW_ASSERTION_ERROR.fail(msg, cause);
    } catch (AssertionError expected) {
      assertThat(expected).hasMessage(msg);
      assertThat(expected.getCause()).isSameAs(cause);
    }
  }

  @Test public void equalsThrowsUSOE() {
    try {
      assertThat(5).equals(5);
    } catch (UnsupportedOperationException expected) {
      assertThat(expected).hasMessage(
          "If you meant to test object equality, use .isEqualTo(other) instead.");
      return;
    }
    fail("Should have thrown.");
  }

  @Test public void hashCodeThrowsUSOE() {
    try {
      assertThat(5).hashCode();
    } catch (UnsupportedOperationException expected) {
      assertThat(expected).hasMessage("Subject.hashCode() is not supported.");
      return;
    }
    fail("Should have thrown.");
  }

  private static <T> Iterable<T> oneShotIterable(final T... values) {
    final Iterator<T> iterator = Iterators.forArray(values);
    return new Iterable<T>() {
      @Override public Iterator<T> iterator() {
        return iterator;
      }
      @Override public String toString() {
        return Arrays.toString(values);
      }
    };
  }
}