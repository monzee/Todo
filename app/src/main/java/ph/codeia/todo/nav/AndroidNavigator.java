package ph.codeia.todo.nav;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class AndroidNavigator implements Navigator {
    private final FragmentActivity activity;
    private final Bundle bundle = new Bundle();

    public AndroidNavigator(FragmentActivity activity) {
        this.activity = activity;
    }

    @Override
    public Navigator param(String name, int value) {
        bundle.putInt(name, value);
        return this;
    }

    @Override
    public Navigator param(String name, String value) {
        bundle.putString(name, value);
        return this;
    }

    @Override
    public void go(To destination) {
        switch (destination) {
            case LIST:
                break;
            case DETAIL:
                break;
            case ENTRY:
                break;
            case STATS:
                break;
        }
        bundle.clear();
    }
}
