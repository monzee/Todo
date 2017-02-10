package ph.codeia.todo.nav;

public interface Navigator {

    Navigator param(String name, int value);
    Navigator param(String name, String value);
    void go(To destination);

    enum To { LIST, DETAIL, ENTRY, STATS }

}
