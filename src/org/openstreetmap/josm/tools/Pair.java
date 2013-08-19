// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;
import java.util.ArrayList;

/**
 * A pair of objects.
 * @param <A> Type of first item
 * @param <B> Type of second item
 * @since 429
 */
public final class Pair<A,B> {
    
    /**
     * The first item
     */
    public A a;
    
    /**
     * The second item
     */
    public B b;

    /**
     * Constructs a new {@code Pair}.
     * @param a The first item
     * @param b The second item
     */
    public Pair(A a, B b) {
        this.a = a;
        this.b = b;
    }

    @Override public int hashCode() {
        return a.hashCode() + b.hashCode();
    }

    @Override public boolean equals(Object other) {
        if (other instanceof Pair<?, ?>) {
            Pair<?, ?> o = (Pair<?, ?>)other;
            return a.equals(o.a) && b.equals(o.b);
        } else
            return false;
    }

    public static <T> ArrayList<T> toArrayList(Pair<T, T> p) {
        ArrayList<T> l = new ArrayList<T>(2);
        l.add(p.a);
        l.add(p.b);
        return l;
    }

    public static <T> Pair<T,T> sort(Pair<T,T> p) {
        if (p.b.hashCode() < p.a.hashCode()) {
            T tmp = p.a;
            p.a = p.b;
            p.b = tmp;
        }
        return p;
    }

    @Override
    public String toString() {
        return "<"+a+","+b+">";
    }

    /**
     * Convenient constructor method 
     * @param u The first item
     * @param v The second item
     * @return The newly created Pair(u,v)
     */
    public static <U,V> Pair<U,V> create(U u, V v) {
        return new Pair<U,V>(u,v);
    }
}
