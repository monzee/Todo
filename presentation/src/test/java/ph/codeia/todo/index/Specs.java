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
import ph.codeia.todo.Repeating;
import ph.codeia.todo.Stepper;
import ph.codeia.todo.data.TodoInMemory;
import ph.codeia.todo.data.TodoRepository;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.*;

@SuppressWarnings("StatementWithEmptyBody")
public class Specs {

    @Rule
    public Timeout noDeadlocks = new Timeout(1000, TimeUnit.MILLISECONDS);

    Stepper<Index.State, Index.Action, Index.View> index;
    Index.Presenter p;
    FakeView v;
    TodoRepository m;

    @Before
    public void setup() {
        index = new Stepper<>(new Index.State(true, true, Collections.emptyList()));
        m = new TodoInMemory();
        p = new IndexActions(m);
        v = new FakeView();
    }

    @Test
    public void should_show_spinner_before_fetching() {
        assertFalse(v.isSpinning);
        index.apply(p.load(), v);
        assertTrue(v.isSpinning);
    }

    @Test
    public void should_hide_spinner_after_fetching() {
        index.apply(p.load(), v);
        while (index.step(v));
        assertFalse(v.isSpinning);
    }

    @Test
    public void should_build_cache_immediately_after_fetching() {
        m.add("foo", "FOO", false);
        m.add("bar", "BAR", true);
        m.add("baz", "BAZ", false);

        index.apply(p.load(), v);
        assertTrue(index.step(v));
        assertEquals(3, index.state().cache.size());
    }

    @Test
    public void should_show_all_items_after_fetching() {
        m.add("foo", "FOO", false);
        m.add("bar", "BAR", true);
        m.add("baz", "BAZ", false);

        index.apply(p.load(), v);
        index.step(v);

        assertEquals(0, v.count());
        assertTrue(index.step(v));  // reload
        assertEquals(3, v.count());
        m.all().stream()
                .mapToInt(e -> e.id)
                .forEach(id -> assertThat(visibleIds(), hasItem(id)));
    }

    @Test
    public void should_update_a_single_item_when_its_status_is_changed() {
        TodoRepository.Todo e1, e2;
        m.add("extra", "FOO", false);
        e1 = m.add("bar", "BAR", true);
        m.add("extra", "FOO", false);
        m.add("extra", "FOO", false);
        m.add("extra", "FOO", false);
        e2 = m.add("baz", "BAZ", false);
        m.add("extra", "FOO", false);
        m.add("extra", "FOO", false);
        m.add("extra", "FOO", false);

        index.apply(p.load(), v);
        while (index.step(v));
        index.apply(p.toggle(e1.id, !e1.completed), v);
        index.apply(p.toggle(e2.id, !e2.completed), v);
        while (index.step(v));
        Optional<Index.Item> item = v.s(i -> i.id() == e1.id).findFirst();
        assertTrue(item.isPresent());
        assertNotEquals(e1.completed, item.get().completed());
        item = v.s(i -> i.id() == e2.id).findFirst();
        assertTrue(item.isPresent());
        assertNotEquals(e2.completed, item.get().completed());
    }

    @Test
    public void should_hide_all_completed_when_the_completed_filter_is_unchecked() {
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

        index.apply(p.load(), v);
        while (index.step(v));
        assertEquals(12, v.count());

        index.apply(p.filter(false, true), v);
        while (index.step(v));
        assertEquals(9, v.count());
        complete.stream().forEach(id -> assertThat(visibleIds(), not(hasItem(id))));
    }

    @Test
    public void should_hide_all_active_when_the_active_filter_is_unchecked() {
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

        index.apply(p.load(), v);
        while (index.step(v));
        assertEquals(12, v.count());

        index.apply(p.filter(true, false), v);
        while (index.step(v));
        assertEquals(9, v.count());
        active.stream().forEach(id -> assertThat(visibleIds(), not(hasItem(id))));
    }

    @Test
    public void should_delete_from_repo_all_completed_when_asked() {
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

        index.apply(p.load(), v);
        while (index.step(v));
        index.apply(p.deleteCompleted(), v);
        v.confirm(index);
        while (index.step(v));

        List<Integer> allIds = m.all().stream().map(e -> e.id).collect(Collectors.toList());
        assertEquals(9, allIds.size());
        complete.stream().forEach(id -> assertThat(allIds, not(hasItem(id))));
    }

    List<Integer> visibleIds() {
        return v.s()
                .map(Repeating.Diffable::id)
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
                unit.apply(confirm, this, Runnable::run);
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
