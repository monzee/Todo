package ph.codeia.todo.data;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;


public class PersistentRepoTest {

    @Rule
    public TemporaryFolder dir = new TemporaryFolder();

    @Test
    public void strawman() throws IOException, ClassNotFoundException {
        File file = dir.newFile();
        assertTrue(file.delete());
        TodoRepository repo = new TodoSerialized(file);
        try (TodoSerialized write = repo.transact()) {
            write.add("first", "first entry", true);
            write.add("second", "second entry", false);
            write.add("third", "third entry", true);
        }

        repo = new TodoSerialized(file);
        TodoRepository.Todo e;
        e = repo.oneWithId(1);
        assertEquals("first", e.title);
        assertEquals("first entry", e.description);
        assertTrue(e.completed);
        e = repo.oneWithId(2);
        assertEquals("second", e.title);
        assertEquals("second entry", e.description);
        assertFalse(e.completed);
        e = repo.oneWithId(3);
        assertEquals("third", e.title);
        assertEquals("third entry", e.description);
        assertTrue(e.completed);
    }

    @Test
    public void can_save_outside_a_transaction() throws IOException, ClassNotFoundException {
        File file = dir.newFile();
        assertTrue(file.delete());
        TodoRepository repo = new TodoSerialized(file);
        TodoRepository.Todo e;

        repo.add("asdf", "zxcv", false);
        e = repo.oneWithId(1);
        assertEquals("asdf", e.title);
        assertEquals("zxcv", e.description);
        assertFalse(e.completed);

        repo.add("qwer", "asdf", true);
        e = repo.oneWithId(2);
        assertEquals("qwer", e.title);
        assertEquals("asdf", e.description);
        assertTrue(e.completed);
    }

    @Test
    public void can_edit_row() throws IOException, ClassNotFoundException {
        File file = dir.newFile();
        assertTrue(file.delete());
        TodoRepository repo = new TodoSerialized(file);
        TodoRepository.Todo e;

        repo.add("asdf", "qwer", false);
        e = repo.oneWithId(1);
        assertNotNull(e);

        repo.put(e.withDescription("zxcv").withCompleted(true));
        e = repo.oneWithId(1);
        assertEquals("zxcv", e.description);
        assertTrue(e.completed);
    }

    @Test
    public void remembers_last_row_id_assigned() throws IOException, ClassNotFoundException {
        File file = dir.newFile();
        assertTrue(file.delete());
        TodoRepository repo;
        TodoRepository.Todo e;

        repo = new TodoSerialized(file);
        try (TodoRepository.Transactional r = repo.transact()) {
            r.add("foo", "abc", false);
            r.add("bar", "def", false);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        assertNotNull(repo.oneWithId(2));
        repo.delete(2);
        assertNull(repo.oneWithId(2));

        repo = new TodoSerialized(file);
        try (TodoSerialized r = repo.transact()) {
            r.add("baz", "abc", false);
        }
        e = repo.oneWithId(3);
        assertNotNull(e);
        assertEquals("baz", e.title);
    }

    void out(TodoRepository repo) {
        for (TodoRepository.Todo todo : repo.all()) {
            String s = String.format("#%d: [%s] %s - %s",
                    todo.id,
                    todo.completed ? 'x' : ' ',
                    todo.title.toUpperCase(),
                    todo.description);
            System.out.println(s);
        }
    }
}
