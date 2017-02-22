package ph.codeia.todo.util;

public final class Pattern {

    public interface Maybe<T> {
        void present(T t);
        void absent();
    }

    public interface Io<T> {
        void apply(T t);
    }

    public static <T> Maybe<T> whenPresent(Io<? super T> consumer) {
        return new Maybe<T>() {
            @Override
            public void present(T t) {
                consumer.apply(t);
            }

            @Override
            public void absent() {}
        };
    }

    public static <T> Maybe<T> whenAbsent(Runnable block) {
        return new Maybe<T>() {
            @Override
            public void present(T o) {}

            @Override
            public void absent() {
                block.run();
            }
        };
    }

    private Pattern() {}
}
