package adco.helixsongview.customviews;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import adco.helixsongview.screensetup.Palette;
import com.google.android.material.button.MaterialButtonToggleGroup;

public class MyMaterialButtonToggleGroup extends MaterialButtonToggleGroup {
    public MyMaterialButtonToggleGroup(@NonNull Context context) {
        this(context,null);
    }

    public MyMaterialButtonToggleGroup(@NonNull Context context, @Nullable @org.jetbrains.annotations.Nullable AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode()) {
            setPalette(new Palette(context));
        }
    }

    public void setPalette(Palette palette) {
        setBackgroundColor(palette.background);
    }
}
