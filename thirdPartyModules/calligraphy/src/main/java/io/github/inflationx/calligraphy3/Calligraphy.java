package io.github.inflationx.calligraphy3;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

class Calligraphy {

    private static final String ACTION_BAR_TITLE = "action_bar_title";
    private static final String ACTION_BAR_SUBTITLE = "action_bar_subtitle";

    /**
     * Some styles are in sub styles, such as actionBarTextStyle etc..
     *
     * @param view view to check.
     * @return 2 element array, default to -1 unless a style has been found.
     */
    protected int[] getStyleForTextView(TextView view) {
        final int[] styleIds = new int[]{-1, -1};
        // Try to find the specific actionbar styles
        if (isActionBarTitle(view)) {
            styleIds[0] = android.R.attr.actionBarStyle;
            styleIds[1] = android.R.attr.titleTextStyle;
        } else if (isActionBarSubTitle(view)) {
            styleIds[0] = android.R.attr.actionBarStyle;
            styleIds[1] = android.R.attr.subtitleTextStyle;
        }
        if (styleIds[0] == -1) {
            // Use TextAppearance as default style
            styleIds[0] = mCalligraphyConfig.getClassStyles().containsKey(view.getClass())
                    ? mCalligraphyConfig.getClassStyles().get(view.getClass())
                    : android.R.attr.textAppearance;
        }
        return styleIds;
    }

    /**
     * An even dirtier way to see if the TextView is part of the ActionBar
     *
     * @param view TextView to check is Title
     * @return true if it is.
     */
    @SuppressLint("NewApi")
    protected static boolean isActionBarTitle(TextView view) {
        if (matchesResourceIdName(view, ACTION_BAR_TITLE)) return true;
        if (parentIsToolbarV7(view)) {
            final Toolbar parent = (Toolbar) view.getParent();
            return TextUtils.equals(parent.getTitle(), view.getText());
        }
        return false;
    }

    /**
     * An even dirtier way to see if the TextView is part of the ActionBar
     *
     * @param view TextView to check is Title
     * @return true if it is.
     */
    @SuppressLint("NewApi")
    protected static boolean isActionBarSubTitle(TextView view) {
        if (matchesResourceIdName(view, ACTION_BAR_SUBTITLE)) return true;
        if (parentIsToolbarV7(view)) {
            final Toolbar parent = (Toolbar) view.getParent();
            return TextUtils.equals(parent.getSubtitle(), view.getText());
        }
        return false;
    }

    protected static boolean parentIsToolbarV7(View view) {
        return CalligraphyUtils.canCheckForV7Toolbar() && view.getParent() != null && (view.getParent() instanceof Toolbar);
    }

    /**
     * Use to match a view against a potential view id. Such as ActionBar title etc.
     *
     * @param view    not null view you want to see has resource matching name.
     * @param matches not null resource name to match against. Its not case sensitive.
     * @return true if matches false otherwise.
     */
    protected static boolean matchesResourceIdName(View view, String matches) {
        if (view.getId() == View.NO_ID) return false;
        final String resourceEntryName = view.getResources().getResourceEntryName(view.getId());
        return resourceEntryName.equalsIgnoreCase(matches);
    }

    private final CalligraphyConfig mCalligraphyConfig;
    private final int[] mAttributeId;

    public Calligraphy(CalligraphyConfig calligraphyConfig) {
        mCalligraphyConfig = calligraphyConfig;
        this.mAttributeId = new int[]{calligraphyConfig.getAttrId()};
    }

    /**
     * Handle the created view
     *
     * @param view    nullable.
     * @param context shouldn't be null.
     * @param attrs   shouldn't be null.
     * @return null if null is passed in.
     */

    public View onViewCreated(View view, Context context, AttributeSet attrs) {
        if (view != null && view.getTag(R.id.calligraphy_tag_id) != Boolean.TRUE) {
            onViewCreatedInternal(view, context, attrs);
            view.setTag(R.id.calligraphy_tag_id, Boolean.TRUE);
        }
        return view;
    }

    void onViewCreatedInternal(View view, final Context context, AttributeSet attrs) {
        if (view instanceof TextView) {
            // Fast path the setting of TextView's font, means if we do some delayed setting of font,
            // which has already been set by use we skip this TextView (mainly for inflating custom,
            // TextView's inside the Toolbar/ActionBar).
            if (TypefaceUtils.isLoaded(((TextView) view).getTypeface())) {
                return;
            }
            // Try to get typeface attribute value
            // Since we're not using namespace it's a little bit tricky

            // Check xml attrs, style attrs and text appearance for font path
            String textViewFont = resolveFontPath(context, attrs);

            // Try theme attributes
            if (TextUtils.isEmpty(textViewFont)) {
                final int[] styleForTextView = getStyleForTextView((TextView) view);
                if (styleForTextView[1] != -1)
                    textViewFont = CalligraphyUtils.pullFontPathFromTheme(context, styleForTextView[0], styleForTextView[1], mAttributeId);
                else
                    textViewFont = CalligraphyUtils.pullFontPathFromTheme(context, styleForTextView[0], mAttributeId);
            }

            textViewFont = applyFontMapper(textViewFont);

            // Still need to defer the Native action bar, appcompat-v7:21+ uses the Toolbar underneath. But won't match these anyway.
            final boolean deferred = matchesResourceIdName(view, ACTION_BAR_TITLE) || matchesResourceIdName(view, ACTION_BAR_SUBTITLE);

            CalligraphyUtils.applyFontToTextView(context, (TextView) view, mCalligraphyConfig, textViewFont, deferred);
        }

        // AppCompat API21+ The ActionBar doesn't inflate default Title/SubTitle, we need to scan the
        // Toolbar(Which underlies the ActionBar) for its children.
        if (CalligraphyUtils.canCheckForV7Toolbar() && view instanceof Toolbar) {
            final Toolbar toolbar = (Toolbar) view;
            toolbar.getViewTreeObserver().addOnGlobalLayoutListener(new ToolbarLayoutListener(this, context, toolbar));
        }

        // Try to set typeface for custom views using interface method or via reflection if available
        if (view instanceof HasTypeface) {
            String textViewFont = resolveFontPath(context, attrs);
            textViewFont = applyFontMapper(textViewFont);
            Typeface typeface = getDefaultTypeface(context, textViewFont);
            if (typeface != null) {
                ((HasTypeface) view).setTypeface(typeface);
            }
        } else if (mCalligraphyConfig.isCustomViewTypefaceSupport() && mCalligraphyConfig.isCustomViewHasTypeface(view)) {
            final Method setTypeface = ReflectionUtils.getMethod(view.getClass(), "setTypeface");
            String fontPath = resolveFontPath(context, attrs);
            fontPath = applyFontMapper(fontPath);
            Typeface typeface = getDefaultTypeface(context, fontPath);
            if (setTypeface != null && typeface != null) {
                ReflectionUtils.invokeMethod(view, setTypeface, typeface);
            }
        }

    }

    private Typeface getDefaultTypeface(Context context, String fontPath) {
        if (TextUtils.isEmpty(fontPath)) {
            fontPath = mCalligraphyConfig.getFontPath();
        }
        if (!TextUtils.isEmpty(fontPath)) {
            return TypefaceUtils.load(context.getAssets(), fontPath);
        }
        return null;
    }

    /**
     * Resolving font path from xml attrs, style attrs or text appearance
     */
    private String resolveFontPath(Context context, AttributeSet attrs) {
        // Try view xml attributes
        String textViewFont = CalligraphyUtils.pullFontPathFromView(context, attrs, mAttributeId);

        // Try view style attributes
        if (TextUtils.isEmpty(textViewFont)) {
            textViewFont = CalligraphyUtils.pullFontPathFromStyle(context, attrs, mAttributeId);
        }

        // Try View TextAppearance
        if (TextUtils.isEmpty(textViewFont)) {
            textViewFont = CalligraphyUtils.pullFontPathFromTextAppearance(context, attrs, mAttributeId);
        }

        return textViewFont;
    }

    private String applyFontMapper(String textViewFont) {
        FontMapper fontMapper = mCalligraphyConfig.getFontMapper();
        return fontMapper != null ? fontMapper.map(textViewFont) : textViewFont;
    }

    private static class ToolbarLayoutListener implements ViewTreeObserver.OnGlobalLayoutListener {

        static String BLANK = " ";

        private final WeakReference<Calligraphy> mCalligraphyFactory;
        private final WeakReference<Context> mContextRef;
        private final WeakReference<Toolbar> mToolbarReference;
        private final CharSequence originalSubTitle;

        private ToolbarLayoutListener(final Calligraphy calligraphy,
                                      final Context context, Toolbar toolbar) {
            mCalligraphyFactory = new WeakReference<>(calligraphy);
            mContextRef = new WeakReference<>(context);
            mToolbarReference = new WeakReference<>(toolbar);
            originalSubTitle = toolbar.getSubtitle();
            toolbar.setSubtitle(BLANK);
        }

        @Override public void onGlobalLayout() {
            final Toolbar toolbar = mToolbarReference.get();
            final Context context = mContextRef.get();
            final Calligraphy factory = mCalligraphyFactory.get();
            if (toolbar == null) return;
            if (factory == null || context == null) {
                removeSelf(toolbar);
                return;
            }

            int childCount = toolbar.getChildCount();
            if (childCount != 0) {
                // Process children, defer draw as it has set the typeface.
                for (int i = 0; i < childCount; i++) {
                    factory.onViewCreated(toolbar.getChildAt(i), context, null);
                }
            }
            removeSelf(toolbar);
            CharSequence subTitle = toolbar.getSubtitle();
            if (subTitle == null || BLANK.equals(subTitle.toString())) {
                toolbar.setSubtitle(originalSubTitle);
            } else {
                toolbar.setSubtitle(subTitle);
            }
        }

        private void removeSelf(final Toolbar toolbar) {// Our dark deed is done
            toolbar.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        }

    }

}
