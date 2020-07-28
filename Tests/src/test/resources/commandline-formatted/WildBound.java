package src.test.resources;
class WildBound {
    <T> T f() {
        return (T)X.class.newInstance((F<?>)f, t);
    }
}
