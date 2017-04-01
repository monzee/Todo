package ph.codeia.todo;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.util.Log;

public class BaseFragment extends Fragment {

    protected static final String UP_TO_DATE = "is-not-stale";
    protected static final String ABNORMAL_EXIT = "murdered-by-the-phone";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            TransitionInflater inflater = TransitionInflater.from(getContext());
            Transition transition = inflater.inflateTransition(R.transition.move_text);
            setSharedElementEnterTransition(transition);
            setSharedElementReturnTransition(transition);
            setReenterTransition(inflater.inflateTransition(android.R.transition.fade));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Bundle args = getArguments();
        if (args != null) {
            args.remove(ABNORMAL_EXIT);
        }
    }

    public void log(Mvp.Log level, String message) {
        String suffix = getClass().getSimpleName();
        switch (level) {
            case D:
                Log.d("mz:" + suffix, message);
                break;
            case I:
                Log.i("mz:" + suffix, message);
                break;
            case E:
                Log.e("mz:" + suffix, message);
                break;
        }
    }

    public void log(Mvp.Log level, Throwable error) {
        String suffix = getClass().getSimpleName();
        switch (level) {
            case D:
                Log.d("mz:" + suffix, "Caught:", error);
                break;
            case I:
                Log.i("mz:" + suffix, "Caught:", error);
                break;
            case E:
                Log.e("mz:" + suffix, "Caught:", error);
                break;
        }
    }

    protected boolean shouldLoad() {
        return shouldLoad(null);
    }

    protected boolean shouldLoad(@Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        if (args == null) {
            return savedInstanceState == null;
        }
        boolean isStale = !args.getBoolean(UP_TO_DATE, false) ||
                args.containsKey(ABNORMAL_EXIT);
        args.putBoolean(UP_TO_DATE, true);
        args.putBoolean(ABNORMAL_EXIT, true);
        return isStale;
    }

    protected void forceLoadOnReenter() {
        Bundle args = getArguments();
        if (args != null) {
            args.putBoolean(UP_TO_DATE, false);
        }
    }
}
