package ph.codeia.todo.util;

import android.annotation.SuppressLint;
import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

public abstract class FragmentProvider<F extends Fragment> {

    public static class Builder<F extends Fragment> {
        private final Class<F> cls;
        private final String tag;

        public Builder(Class<F> cls, String tag) {
            this.cls = cls;
            this.tag = tag;
        }

        public FragmentProvider<F> build(FragmentActivity activity) {
            return new FragmentProvider<F>(activity.getSupportFragmentManager(), tag) {
                @SuppressWarnings("unchecked")
                @Override
                public F instance() {
                    Fragment f = fm.findFragmentByTag(tag);
                    if (f != null && cls.isInstance(f)) {
                        return (F) f;
                    }
                    return (F) F.instantiate(activity, cls.getCanonicalName());
                }

                @SuppressWarnings("unchecked")
                @Override
                public void check(Pattern.Maybe<F> matcher) {
                    Fragment f = fm.findFragmentByTag(tag);
                    if (f != null && cls.isInstance(f)) {
                        matcher.present((F) f);
                    } else {
                        matcher.absent();
                    }
                }
            };
        }
    }

    protected final FragmentManager fm;
    protected final String tag;

    public FragmentProvider(FragmentManager fm, String tag) {
        this.fm = fm;
        this.tag = tag;
    }

    public abstract F instance();
    public abstract void check(Pattern.Maybe<F> matcher);

    public void get(Pattern.Io<F> block) {
        check(Pattern.whenPresent(block));
    }

    @SuppressLint("CommitTransaction")
    public FragmentTransaction replace(@IdRes int id) {
        return replace(fm.beginTransaction(), id);
    }

    public FragmentTransaction replace(FragmentTransaction transaction, @IdRes int id) {
        return transaction.replace(id, instance(), tag);
    }

    @SuppressLint("CommitTransaction")
    public FragmentTransaction add() {
        return add(fm.beginTransaction());
    }

    public FragmentTransaction add(FragmentTransaction transaction) {
        return transaction.add(instance(), tag);
    }

}
