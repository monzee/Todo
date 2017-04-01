package ph.codeia.todo.index;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ph.codeia.todo.Mvp;
import ph.codeia.todo.Stepper;
import ph.codeia.todo.data.TodoInMemory;
import ph.codeia.todo.data.TodoRepository;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class IndexSpecs {

    @Rule
    public Timeout noDeadlocks = new Timeout(1000, TimeUnit.MILLISECONDS);

    Stepper<Index.State, Index.Action, Index.View> index;
    Index.Presenter p;
    FakeView v;
    TodoRepository m;

    @Before
    public void setup() {
        index = new Stepper<>(Index.State.ROOT);
        m = new TodoInMemory();
        p = new IndexActions(m);
        v = new FakeView();
    }

    @Test
    public void should_show_spinner_before_fetching() {
        assertFalse(v.isSpinning);
        index.apply(v, p.load());
        assertTrue(v.isSpinning);
    }

    @Test
    public void should_hide_spinner_after_fetching() {
        index.apply(v, p.load());
        index.drain(v);
        assertFalse(v.isSpinning);
    }

    @Test
    public void should_load_fetched_items_into_cache() {
        m.add("foo", "FOO", false);
        m.add("bar", "BAR", true);
        m.add("baz", "BAZ", false);

        assertThat(index.state().cache, not(hasItem(anything())));
        index.apply(v, p.load());
        assertTrue(index.step(v));
        assertEquals(3, index.state().cache.size());
    }

    @Test
    public void should_show_all_items_after_fetching() {
        m.add("foo", "FOO", false);
        m.add("bar", "BAR", true);
        m.add("baz", "BAZ", false);

        index.apply(v, p.load());
        index.step(v);

        assertEquals(0, v.count());
        assertTrue(index.step(v));  // reload
        assertEquals(3, v.count());
        m.all().stream()
                .mapToInt(e -> e.id)
                .forEach(id -> assertThat(visibleIds(), hasItem(id)));
    }

    @Test
    public void should_reflect_status_changes_in_repo() {
        TodoRepository.Todo e1, e2;
        Optional<Index.Item> item;
        m.add("extra", "FOO", false);
        e1 = m.add("bar", "BAR", true);
        m.add("extra", "FOO", false);
        m.add("extra", "FOO", false);
        index.apply(v, p.load());
        index.drain(v);

        index.apply(v, p.setCompleted(e1.id, !e1.completed));
        index.drain(v);
        item = v.s(i -> i.id() == e1.id).findFirst();
        assertTrue(item.isPresent());
        assertNotEquals(e1.completed, item.get().completed());

        m.add("extra", "FOO", false);
        m.add("extra", "FOO", false);
        e2 = m.add("baz", "BAZ", false);
        m.add("extra", "FOO", false);
        m.add("extra", "FOO", false);
        index.apply(v, p.load());
        index.drain(v);

        index.apply(v, p.setCompleted(e2.id, !e2.completed));
        index.drain(v);
        item = v.s(i -> i.id() == e2.id).findFirst();
        assertTrue(item.isPresent());
        assertNotEquals(e2.completed, item.get().completed());
    }

    @Test
    public void should_hide_all_completed_items_when_filtered_out() {
        List<Integer> complete = new ArrayList<>();
        m.add("extra", "FOO", false);
        m.add("extra", "FOO", false);
        complete.add(m.add("foo", "FOO", true).id);
        m.add("extra", "FOO", false);
        m.add("extra", "FOO", false);
        m.add("extra", "FOO", false);
        complete.add(m.add("bar", "BAR", true).id);
        m.add("extra", "FOO", false);
        m.add("extra", "FOO", false);
        complete.add(m.add("baz", "BAZ", true).id);
        m.add("extra", "FOO", false);
        m.add("extra", "FOO", false);

        index.apply(v, p.load());
        index.drain(v);
        assertEquals(12, v.count());

        index.apply(v, p.filter(false, true));
        index.drain(v);
        assertEquals(9, v.count());
        complete.forEach(id -> assertThat(visibleIds(), not(hasItem(id))));
    }

    @Test
    public void should_hide_all_active_items_when_filtered_out() {
        List<Integer> active = new ArrayList<>();
        m.add("extra", "FOO", true);
        m.add("extra", "FOO", true);
        active.add(m.add("foo", "FOO", false).id);
        m.add("extra", "FOO", true);
        m.add("extra", "FOO", true);
        m.add("extra", "FOO", true);
        active.add(m.add("bar", "BAR", false).id);
        m.add("extra", "FOO", true);
        m.add("extra", "FOO", true);
        active.add(m.add("baz", "BAZ", false).id);
        m.add("extra", "FOO", true);
        m.add("extra", "FOO", true);

        index.apply(v, p.load());
        index.drain(v);
        assertEquals(12, v.count());

        index.apply(v, p.filter(true, false));
        index.drain(v);
        assertEquals(9, v.count());
        active.forEach(id -> assertThat(visibleIds(), not(hasItem(id))));
    }

    @Test
    public void should_be_able_to_delete_all_completed_items_from_repo() {
        List<Integer> complete = new ArrayList<>();
        m.add("extra", "FOO", false);
        m.add("extra", "FOO", false);
        complete.add(m.add("foo", "FOO", true).id);
        m.add("extra", "FOO", false);
        m.add("extra", "FOO", false);
        m.add("extra", "FOO", false);
        complete.add(m.add("bar", "BAR", true).id);
        m.add("extra", "FOO", false);
        m.add("extra", "FOO", false);
        complete.add(m.add("baz", "BAZ", true).id);
        m.add("extra", "FOO", false);
        m.add("extra", "FOO", false);

        index.apply(v, p.load());
        index.drain(v);
        index.apply(v, p.deleteAllCompleted());
        v.confirm(index);
        index.drain(v);

        List<Integer> allIds = m.all().stream().map(e -> e.id).collect(Collectors.toList());
        assertEquals(9, allIds.size());
        complete.forEach(id -> assertThat(allIds, not(hasItem(id))));
    }

    List<Integer> visibleIds() {
        return v.s()
                .map(Index.Item::id)
                .map(Long::intValue)
                .collect(Collectors.toList());
    }

    static class FakeView implements Index.View {
        boolean isSpinning;
        List<Index.Item> visible = Collections.emptyList();
        Index.Action confirm;

        int count() {
            return visible.size();
        }

        Stream<Index.Item> s() {
            return visible.stream();
        }

        Stream<Index.Item> s(Predicate<Index.Item> where) {
            return s().filter(where);
        }

        void confirm(Mvp.Unit<Index.State, Index.Action, Index.View> unit) {
            if (confirm != null) {
                unit.apply(Runnable::run, this, confirm);
                confirm = null;
            }
        }

        @Override
        public void log(Mvp.Log level, String message) {
            if (level == Mvp.Log.E) {
                System.err.println(message);
            } else {
                System.out.println(message);
            }
        }

        @Override
        public void log(Mvp.Log level, Throwable error) {
            error.printStackTrace();
        }

        @Override
        public void tell(String message, Object... fmtArgs) {
            System.out.println(String.format(message, fmtArgs));
        }

        @Override
        public void spin(boolean busy) {
            isSpinning = busy;
        }

        @Override
        public void confirmDelete(Index.Action onConfirm) {
            confirm = onConfirm;
        }

        @Override
        public void show(List<Index.Item> items) {
            visible = items;
        }

        @Override
        public void goToEntryForm() {
        }

        @Override
        public void goToDetails(int id) {
        }
    }
}
