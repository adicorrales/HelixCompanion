package adco.helixsongview.setmenu;

import android.annotation.SuppressLint;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import adco.helixsongview.R;
import adco.helixsongview.customviews.MyMaterialCheckbox;
import adco.helixsongview.customviews.MyMaterialSimpleTextView;
import adco.helixsongview.interfaces.MainActivityInterface;
import adco.helixsongview.interfaces.SetItemTouchInterface;
import com.google.android.material.card.MaterialCardView;

public class SelectSetViewHolder extends RecyclerView.ViewHolder implements View.OnTouchListener,
        GestureDetector.OnGestureListener {

    final MaterialCardView cardView;
    final MyMaterialSimpleTextView numberView, titleView, folderView;
    final MyMaterialCheckbox checkBox;
    private final ItemTouchHelper itemTouchHelper;
    private final SetItemTouchInterface setItemTouchInterface;
    private final MainActivityInterface mainActivityInterface;
    private final GestureDetector gestureDetector;

    public SelectSetViewHolder(@NonNull View itemView, MainActivityInterface mainActivityInterface,
                               ItemTouchHelper itemTouchHelper, SetItemTouchInterface setItemTouchInterface) {
        super(itemView);
        cardView = itemView.findViewById(R.id.select_set_card);
        numberView = itemView.findViewById(R.id.select_set_number);
        titleView = itemView.findViewById(R.id.select_set_title);
        folderView = itemView.findViewById(R.id.select_set_folder);
        checkBox = itemView.findViewById(R.id.select_set_checkbox);

        cardView.setOnTouchListener(this);
        gestureDetector = new GestureDetector(itemView.getContext(), this);
        gestureDetector.setIsLongpressEnabled(false);
        this.itemTouchHelper = itemTouchHelper;
        this.setItemTouchInterface = setItemTouchInterface;
        this.mainActivityInterface = mainActivityInterface;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        boolean handled = gestureDetector.onTouchEvent(motionEvent);
        if (motionEvent.getAction() == MotionEvent.ACTION_UP ||
                motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {
            view.getParent().requestDisallowInterceptTouchEvent(false);
        }
        return handled || motionEvent.getAction() == MotionEvent.ACTION_DOWN;
    }

    @Override
    public boolean onDown(@NonNull MotionEvent motionEvent) {
        return false;
    }

    @Override
    public void onShowPress(@NonNull MotionEvent motionEvent) {}

    @Override
    public boolean onSingleTapUp(@NonNull MotionEvent motionEvent) {
        setItemTouchInterface.onItemClicked(mainActivityInterface, getAbsoluteAdapterPosition());
        motionEvent.setAction(MotionEvent.ACTION_CANCEL);
        gestureDetector.onTouchEvent(motionEvent);
        return true;
    }

    @Override
    public boolean onScroll(@Nullable MotionEvent motionEvent, @NonNull MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

    @Override
    public void onLongPress(@NonNull MotionEvent motionEvent) {}

    @Override
    public boolean onFling(@Nullable MotionEvent motionEvent, @NonNull MotionEvent motionEvent1, float v, float v1) {
        return false;
    }
}
