package org.openstreetmap.josm.tools;
import java.util.ArrayList;

/**
 * A pair.
 */
public final class Pair<A,B> {
    public A a;
    public B b;

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
}
