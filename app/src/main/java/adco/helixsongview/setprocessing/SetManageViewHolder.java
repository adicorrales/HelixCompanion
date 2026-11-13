package adco.helixsongview.setprocessing;

import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import adco.helixsongview.R;
import adco.helixsongview.customviews.MyMaterialCheckbox;
import adco.helixsongview.customviews.MyMaterialSimpleTextView;

public class SetManageViewHolder extends RecyclerView.ViewHolder {

    public final LinearLayout itemLayout;
    public final MyMaterialCheckbox checkBox;
    public final MyMaterialSimpleTextView modifiedDate, itemName;

    public SetManageViewHolder(@NonNull View itemView) {
        super(itemView);
        itemLayout = itemView.findViewById(R.id.itemLayout);
        checkBox = itemView.findViewById(R.id.checkBoxItem);
        checkBox.setClickable(false);
        itemName = itemView.findViewById(R.id.itemName);
        modifiedDate = itemView.findViewById(R.id.modifiedDate);
    }
}
