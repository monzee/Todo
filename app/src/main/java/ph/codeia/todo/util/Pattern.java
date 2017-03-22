package ph.codeia.todo.util;

public final class Pattern {

    public interface Option<T> {
        void some(T t);
        void none();
    }

    public interface Io<T> {
        void apply(T t);
    }

    public static <T> Option<T> forSome(Io<? super T> consumer) {
        return new Option<T>() {
            @Override
            public void some(T t) {
                consumer.apply(t);
            }

            @Override
            public void none() {}
        };
    }

    public static <T> Option<T> forNone(Runnable block) {
        return new Option<T>() {
            @Override
            public void some(T o) {}

            @Override
            public void none() {
                block.run();
            }
        };
    }

    private Pattern() {}
}
