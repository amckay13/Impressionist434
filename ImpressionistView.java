package edu.umd.hcil.impressionistpainter434;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import java.text.MessageFormat;
import java.util.Random;

import android.os.Bundle;
import android.support.v4.view.VelocityTrackerCompat;
import android.view.MotionEvent;
import android.view.VelocityTracker;


/**
 * Created by jon on 3/20/2016.
 */
public class ImpressionistView extends View {

    private ImageView _imageView;
    private VelocityTracker _tracker = null;

    private Canvas _offScreenCanvas = null;
    private Bitmap _offScreenBitmap = null;
    private Paint _paint = new Paint();

    private int _alpha = 150;
    private int _defaultRadius = 25;
    private Point _lastPoint = null;
    private long _lastPointTime = -1;
    private boolean _useMotionSpeedForBrushStrokeSize = true;
    private Paint _paintBorder = new Paint();
    private BrushType _brushType = BrushType.Square;
    private float _minBrushRadius = 5;

    private boolean complementary = false;

    public ImpressionistView(Context context) {
        super(context);
        init(null, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    /**
     * Because we have more than one constructor (i.e., overloaded constructors), we use
     * a separate initialization method
     * @param attrs
     * @param defStyle
     */
    private void init(AttributeSet attrs, int defStyle){

        // Set setDrawingCacheEnabled to true to support generating a bitmap copy of the view (for saving)
        // See: http://developer.android.com/reference/android/view/View.html#setDrawingCacheEnabled(boolean)
        //      http://developer.android.com/reference/android/view/View.html#getDrawingCache()
        this.setDrawingCacheEnabled(true);

        _paint.setColor(Color.RED);
        _paint.setAlpha(_alpha);
        _paint.setAntiAlias(true);
        _paint.setStyle(Paint.Style.FILL);
        _paint.setStrokeWidth(4);

        _paintBorder.setColor(Color.BLACK);
        _paintBorder.setStrokeWidth(3);
        _paintBorder.setStyle(Paint.Style.STROKE);
        _paintBorder.setAlpha(50);

        //_paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh){

        Bitmap bitmap = getDrawingCache();
        Log.v("onSizeChanged", MessageFormat.format("bitmap={0}, w={1}, h={2}, oldw={3}, oldh={4}", bitmap, w, h, oldw, oldh));
        if(bitmap != null) {
            _offScreenBitmap = getDrawingCache().copy(Bitmap.Config.ARGB_8888, true);
            _offScreenCanvas = new Canvas(_offScreenBitmap);
        }
    }

    /**
     * Sets the ImageView, which hosts the image that we will paint in this view
     * @param imageView
     */
    public void setImageView(ImageView imageView){
        _imageView = imageView;
    }

    public Bitmap getDoodle(){
        return _offScreenBitmap;
    }

    public void toggleComplementary() {
        complementary = !complementary;
    }

    /**
     * Sets the brush type. Feel free to make your own and completely change my BrushType enum
     * @param brushType
     */
    public void setBrushType(BrushType brushType){
        _brushType = brushType;
    }

    /**
     * Clears the painting
     */
    public void clearPainting(){
        _offScreenCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(_offScreenBitmap != null) {
            canvas.drawBitmap(_offScreenBitmap, 0, 0, _paint);
        }

        // Draw the border. Helpful to see the size of the bitmap in the ImageView
        canvas.drawRect(getBitmapPositionInsideImageView(_imageView), _paintBorder);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){
        //TODO
        //Basically, the way this works is to listen for Touch Down and Touch Move events and determine where those
        //touch locations correspond to the bitmap in the ImageView. You can then grab info about the bitmap--like the pixel color--
        //at that location
        float touchX = motionEvent.getX();
        float touchY = motionEvent.getY();
        Bitmap image = _imageView.getDrawingCache();

        switch(motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                int color = image.getPixel((int) touchX, (int) touchY);
                Paint p = new Paint();
                p.setColor(color);

                //_offScreenBitmap.setPixel((int) touchX, (int) touchY, color);
                if (_brushType == BrushType.Circle) {
                    _offScreenCanvas.drawCircle((int) touchX, (int) touchY, 13, p);
                } else if (_brushType == BrushType.CircleSplatter) {
                    Random r = new Random();
                    _offScreenCanvas.drawCircle((int) touchX + 13, (int) touchY, r.nextInt(14) + 3, p);
                    _offScreenCanvas.drawCircle((int) touchX, (int) touchY + 13, r.nextInt(14) + 3, p);
                    _offScreenCanvas.drawCircle((int) touchX + 13, (int) touchY + 13, r.nextInt(14) + 3, p);
                    _offScreenCanvas.drawCircle((int) touchX - 13, (int) touchY, r.nextInt(14) + 3, p);
                    _offScreenCanvas.drawCircle((int) touchX - 13, (int) touchY - 13, r.nextInt(14) + 3, p);
                } else { //squares
                    if (_tracker == null) {
                        _tracker = VelocityTracker.obtain();
                    } else {
                        _tracker.clear();
                    }
                    _tracker.addMovement(motionEvent);
                    _offScreenCanvas.drawRect(touchX, touchY, (touchX + 13), (touchY + 13), p);
                }

                break;
            case MotionEvent.ACTION_MOVE:
                int color2 = image.getPixel((int) touchX, (int) touchY);
                if (complementary) {
                    int r = Color.red(color2);
                    int g = Color.green(color2);
                    int b = Color.blue(color2);
                    color2 = Color.rgb((255 - r), (255 - g), (255 - b));
                }
                Paint p2 = new Paint();
                p2.setColor(color2);
               //_offScreenBitmap.setPixel((int) touchX, (int) touchY, color2);
                if (_brushType == BrushType.Circle) {
                    _offScreenCanvas.drawCircle((int) touchX, (int) touchY, 13, p2);
                } else if (_brushType == BrushType.CircleSplatter) {
                    Random r = new Random();
                    for (int i = 1; i < 36; i++) {
                        int rand = r.nextInt(11) + 10;
                            _offScreenCanvas.drawCircle((int) touchX + rand, (int) touchY, r.nextInt(14) + 3, p2);
                            _offScreenCanvas.drawCircle((int) touchX, (int) touchY + rand, r.nextInt(14) + 3, p2);
                            _offScreenCanvas.drawCircle((int) touchX, (int) touchY - rand, r.nextInt(14) + 3, p2);
                            _offScreenCanvas.drawCircle((int) touchX - rand, (int) touchY, r.nextInt(14) + 3, p2);
                            _offScreenCanvas.drawCircle((int) touchX - rand, (int) touchY - rand, r.nextInt(14) + 3, p2);
                            _offScreenCanvas.drawCircle((int) touchX + rand, (int) touchY + rand, r.nextInt(14) + 3, p2);

                    }
                } else {// squares
                    _tracker.addMovement(motionEvent);
                    _tracker.computeCurrentVelocity(1000);
                    int xSpeed = (int) _tracker.getXVelocity();
                    int ySpeed =  (int) _tracker.getYVelocity();
                    int Speed = (xSpeed + ySpeed ) / 2;
                    int x = (Speed / 100) * 5;
                    int y = (Speed / 100) * 5;
                    _offScreenCanvas.drawRect(touchX, touchY, (touchX + x), (touchY - y), p2);
                }
                break;
        }
        invalidate();
        return true;
    }

    /**
     * This method is useful to determine the bitmap position within the Image View. It's not needed for anything else
     * Modified from:
     *  - http://stackoverflow.com/a/15538856
     *  - http://stackoverflow.com/a/26930938
     * @param imageView
     * @return
     */
    private static Rect getBitmapPositionInsideImageView(ImageView imageView){
        Rect rect = new Rect();

        if (imageView == null || imageView.getDrawable() == null) {
            return rect;
        }

        // Get image dimensions
        // Get image matrix values and place them in an array
        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        final float scaleX = f[Matrix.MSCALE_X];
        final float scaleY = f[Matrix.MSCALE_Y];

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        final Drawable d = imageView.getDrawable();
        final int origW = d.getIntrinsicWidth();
        final int origH = d.getIntrinsicHeight();

        // Calculate the actual dimensions
        final int widthActual = Math.round(origW * scaleX);
        final int heightActual = Math.round(origH * scaleY);

        // Get image position
        // We assume that the image is centered into ImageView
        int imgViewW = imageView.getWidth();
        int imgViewH = imageView.getHeight();

        int top = (int) (imgViewH - heightActual)/2;
        int left = (int) (imgViewW - widthActual)/2;

        rect.set(left, top, left + widthActual, top + heightActual);

        return rect;
    }
}

