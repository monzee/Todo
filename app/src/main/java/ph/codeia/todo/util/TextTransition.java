package ph.codeia.todo.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.transition.Transition;
import android.transition.TransitionValues;
import android.util.AttributeSet;
import android.util.Property;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.TextView;

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class TextTransition extends Transition {
    private static final String TEXT_SIZE = "mz:transition:textSize";
    private static final String TEXT_COLOR = "mz:transition:textColor";
    private static final String TEXT = "mz:transition:text";

    private static final Property<TextView, Float> TEXT_SIZE_IN_PX =
            new Property<TextView, Float>(Float.class, "textPixelSize") {
                @Override
                public Float get(TextView textView) {
                    return textView.getTextSize();
                }

                @Override
                public void set(TextView object, Float value) {
                    object.setTextSize(TypedValue.COMPLEX_UNIT_PX, value);
                }

                @Override
                public boolean isReadOnly() {
                    return false;
                }
            };

    public TextTransition() {
        super();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public TextTransition(Context context, AttributeSet attribs) {
        super(context, attribs);
    }

    @Override
    public void captureStartValues(TransitionValues transitionValues) {
        if (transitionValues.view instanceof TextView) {
            TextView tv = (TextView) transitionValues.view;
            transitionValues.values.put(TEXT_SIZE, tv.getTextSize());
            transitionValues.values.put(TEXT_COLOR, tv.getCurrentTextColor());
            transitionValues.values.put(TEXT, tv.getText());
            transitionValues.values.put("height", tv.getHeight());
            transitionValues.values.put("clipBounds", tv.getClipBounds());
        }
    }

    @Override
    public void captureEndValues(TransitionValues transitionValues) {
        captureStartValues(transitionValues);
    }

    @Override
    public Animator createAnimator(
            ViewGroup sceneRoot,
            TransitionValues startValues,
            TransitionValues endValues) {
        if (startValues == null || endValues == null || !(endValues.view instanceof TextView)) {
            return null;
        }
        float startSize = (float) startValues.values.get(TEXT_SIZE);
        float endSize = (float) endValues.values.get(TEXT_SIZE);
        int startColor = (int) startValues.values.get(TEXT_COLOR);
        int endColor = (int) endValues.values.get(TEXT_COLOR);
        CharSequence startText = (CharSequence) startValues.values.get(TEXT);
        CharSequence endText = (CharSequence) endValues.values.get(TEXT);
        TextView tv = (TextView) endValues.view;
        ColorStateList endColors = tv.getTextColors();
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, startSize);
        tv.setHeight((int) endValues.values.get("height"));
        tv.setClipBounds((Rect) startValues.values.get("clipBounds"));
        if (TextUtils.isEmpty(endText)) {
            tv.setText(startText);  // prevent flash when end text is asynchronously computed
        }
        ObjectAnimator sizeAnim = ObjectAnimator.ofFloat(tv, TEXT_SIZE_IN_PX, startSize, endSize);
        sizeAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                tv.setClipBounds((Rect) endValues.values.get("clipBounds"));
            }
        });
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return sizeAnim;
        } else {
            tv.setTextColor(startColor);
            ObjectAnimator colorAnim = ObjectAnimator.ofArgb(tv, "textColor", startColor, endColor);
            colorAnim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    tv.setTextColor(endColors);
                }
            });
            AnimatorSet anims = new AnimatorSet();
            anims.playTogether(sizeAnim, colorAnim);
            return anims;
        }
    }
}
