package adco.helixsongview.screensetup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.BatteryManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import adco.helixsongview.customviews.MyMaterialSimpleTextView;
import adco.helixsongview.interfaces.MainActivityInterface;

public class BatteryStatus extends BroadcastReceiver {

    private boolean isCharging;
    @SuppressWarnings({"FieldCanBeLocal","unused"})
    private final String TAG = "BatteryStatus";

    private float batteryTextSize, charge;
    private int batteryDialThickness;
    private final int toolbarHeight;
    private boolean batteryTextOn, batteryDialOn;
    private final MyMaterialSimpleTextView batteryCharge;
    private final ImageView batteryImage;
    private final Context c;
    private final MainActivityInterface mainActivityInterface;
    private boolean isRegistered = false;

    // Bitmap rendering cache parameters to eliminate GC allocation churn
    private BitmapDrawable cachedDrawable;
    private int lastDrawnCharge = -1;
    private boolean lastDrawnIsCharging;
    private int lastDrawnColor;
    private int lastDrawnBgColor;
    private int lastDrawnThickness = -1;
    private int lastDrawnSize = -1;

    public interface MyInterface {
        void setUpBatteryMonitor();
    }

    public BatteryStatus(Context c, ImageView batteryImage, MyMaterialSimpleTextView batteryCharge, int toolbarHeight) {
        this.c = c;
        mainActivityInterface = (MainActivityInterface) c;
        this.batteryImage = batteryImage;
        this.batteryCharge = batteryCharge;
        this.toolbarHeight = toolbarHeight;
    }

    public void setUpBatteryMonitor() {
        // Get the initial preferences
        updateBatteryPrefs();

        if (!isRegistered) {
            try {
                IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                c.registerReceiver(this, intentFilter);
                isRegistered = true;
            } catch (Exception e) {
                Log.e(TAG, "Error registering BatteryStatus receiver", e);
            }
        }

        // Get the initial battery charge and set values and colour
        getBatteryStatus();
    }

    public void tearDownBatteryMonitor() {
        if (isRegistered) {
            try {
                c.unregisterReceiver(this);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering BatteryStatus receiver", e);
            }
            isRegistered = false;
        }
    }

    public void updateBatteryPrefs() {
        setBatteryTextOn(mainActivityInterface.getPreferences().getMyPreferenceBoolean("batteryTextOn",true));
        setBatteryTextSize(mainActivityInterface.getPreferences().getMyPreferenceFloat("batteryTextSize",9.0f));
        setBatteryDialOn(mainActivityInterface.getPreferences().getMyPreferenceBoolean("batteryDialOn",true));
        setBatteryDialThickness(mainActivityInterface.getPreferences().getMyPreferenceInt("batteryDialThickness", 4));
    }

    public void showBatteryStuff(boolean show) {
        if (show) {
            if (batteryImage != null) {
                batteryImage.post(() -> {
                    batteryImage.setVisibility(batteryDialOn ? View.VISIBLE : View.GONE);
                    batteryImage.requestLayout();
                });
            }
            if (batteryCharge != null) {
                batteryCharge.post(() -> {
                    batteryCharge.setVisibility(batteryTextOn ? View.VISIBLE : View.GONE);
                    batteryCharge.requestLayout();
                });
            }
        } else {
            if (batteryImage != null) {
                batteryImage.post(() -> {
                    batteryImage.setVisibility(View.GONE);
                    batteryImage.requestLayout();
                });
            }
            if (batteryCharge != null) {
                batteryCharge.post(() -> {
                    batteryCharge.setVisibility(View.GONE);
                    batteryCharge.requestLayout();
                });
            }
        }
    }

    public void setBatteryDialOn(boolean batteryDialOn) {
        this.batteryDialOn = batteryDialOn;
    }

    public void setBatteryDialThickness(int batteryDialThickness) {
        this.batteryDialThickness = batteryDialThickness;
        setBatteryImage();
    }

    public void setBatteryTextOn(boolean batteryTextOn) {
        this.batteryTextOn = batteryTextOn;
    }

    public void setBatteryTextSize(float batteryTextSize) {
        this.batteryTextSize = batteryTextSize;
        if (batteryCharge != null) {
            batteryCharge.post(() -> batteryCharge.setTextSize(batteryTextSize));
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            charge = scale > 0 ? (level / (float) scale) : 0f;

            // Are we charging / charged?
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;

            try {
                getBatteryStatus();
            } catch (Exception e) {
                Log.e(TAG, "Error in onReceive battery status update", e);
            }
        }
    }

    public void getBatteryStatus() {
        if (batteryTextOn && batteryCharge != null) {
            int i = Math.round(charge * 100.0f);
            String chargeText = i + "%";
            batteryCharge.post(() -> {
                batteryCharge.setTextSize(batteryTextSize);
                batteryCharge.setText(chargeText);
                if (mainActivityInterface != null && mainActivityInterface.getPalette() != null) {
                    batteryCharge.setTextColor(mainActivityInterface.getPalette().textColor);
                }
            });
        }

        // Get the image
        if (batteryImage != null && batteryDialOn && toolbarHeight > 0) {
            batteryImage.post(this::setBatteryImage);
        }
    }

    public void setBatteryImage() {
        BitmapDrawable bmp = batteryImage((int) (charge * 100f));
        if (bmp != null && batteryImage != null) {
            batteryImage.post(() -> batteryImage.setImageDrawable(bmp));
        }
    }

    public BitmapDrawable batteryImage(int charge) {
        int size = (int) (toolbarHeight * 0.75f);
        if (size <= 0) {
            return null;
        }

        // Determine target colors
        int color = mainActivityInterface.getPalette().textColor;
        if (charge > 10 && charge < 16) {
            color = mainActivityInterface.getPalette().hintColor;
        } else if (charge <= 10) {
            color = mainActivityInterface.getPalette().errorColor;
        }

        int bgcolor = mainActivityInterface.getPalette().secondary;
        if (isCharging) {
            bgcolor = 0xff88ff88;
        }

        // Return cached BitmapDrawable if parameters haven't changed
        if (cachedDrawable != null
                && charge == lastDrawnCharge
                && isCharging == lastDrawnIsCharging
                && color == lastDrawnColor
                && bgcolor == lastDrawnBgColor
                && batteryDialThickness == lastDrawnThickness
                && size == lastDrawnSize) {
            return cachedDrawable;
        }

        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bmp = Bitmap.createBitmap(size, size, conf);

        Paint bPaint = new Paint();
        bPaint.setDither(true);
        bPaint.setColor(bgcolor);
        bPaint.setAntiAlias(true);
        bPaint.setStyle(Paint.Style.STROKE);
        bPaint.setStrokeJoin(Paint.Join.ROUND);
        bPaint.setStrokeCap(Paint.Cap.ROUND);
        bPaint.setStrokeWidth(batteryDialThickness);

        Paint mPaint = new Paint();
        mPaint.setDither(true);
        mPaint.setColor(color);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(batteryDialThickness);

        Path circle = new Path();
        RectF box = new RectF(batteryDialThickness, batteryDialThickness,
                size - batteryDialThickness, size - batteryDialThickness);
        float sweep = 360 * charge * 0.01f;
        circle.addArc(box, 270, sweep);

        Path circle2 = new Path();
        RectF box2 = new RectF(batteryDialThickness, batteryDialThickness,
                size - batteryDialThickness, size - batteryDialThickness);
        float sweep2 = 360;
        circle2.addArc(box2, 270, sweep2);

        Canvas canvas = new Canvas(bmp);
        canvas.drawPath(circle2, bPaint);
        canvas.drawPath(circle, mPaint);

        cachedDrawable = new BitmapDrawable(c.getResources(), bmp);
        lastDrawnCharge = charge;
        lastDrawnIsCharging = isCharging;
        lastDrawnColor = color;
        lastDrawnBgColor = bgcolor;
        lastDrawnThickness = batteryDialThickness;
        lastDrawnSize = size;

        return cachedDrawable;
    }

}
