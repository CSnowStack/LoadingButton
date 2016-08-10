package com.cq.loadingbutton;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PathMeasure;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.text.DecimalFormat;

/**
 * Created by cqll on 2016/7/21.
 */
public class LoadingButton extends View {
    private int mRadius, mCircleProgressRadius;
    private int mTextSize;
    private Paint mPaintBg/*背景*/, mPaintText/*文字*/, mPaintProgress/*中间loading*/, mEndCirclePaint/*load消失*/;
    private String mTextContent;
    private int mCenterX, mHeight, mWidth, mMoveWidth = 0;

    private static final long DURATION_NARROW = 800/*变窄的时间*/, DURATION_LOADING = 1500/*中间Loading的时间*/, DURATION_END = 1000/*绘制叉或勾的时间*/, DURATION_RESET = 500/*重置*/;
    //绘制中间的loading
    private PathMeasure mPathMeasure;
    private Path mPath, mDst;
    private float mLength, mCircleAnimatorValue;
    private ValueAnimator mCircleProgressAnimator;

    //绘制最后的状态
    private PathEffect mEffect;
    private Path mTickPath, mTickDst, mCrossPath, mCrossDst;
    private PathMeasure mTickPathMeasure, mCrossPathMeasure;
    private float mTickAnimatorValue, mTickLength, mEndCircleAnimatorValue, mCrossAnimatorValue, mCrossLength;


    //动画进行的程度
    private int mCurrentState = 1;
    private static final int STATE_NORMAL = 1;//正常
    private static final int STATE_CIRCLE_PROGRESS = 2;//中间Loading
    private static final int STATE_LOADING_END = 3;//中间Loading结束

    //是否开始移动去除多余的线
    private boolean mStartRemoveSurplusLine = false;
    private float mRemoveSurplusLineAnimatorValue;

    private int mTickMoveDistance/*勾向右移动的距离*/;

    private DecimalFormat oneDecimalFormat/*一位小数的比较*/, twoDecimalFormat/*两位小数的比较*/;
    private boolean isLoadingSuccess = false, isLoadingFailed = false, isLoading = false;
    private boolean loadingSuccessRest/*加载成功默认不重置*/, loadingFailedReset/*加载失败默认重置*/;

    private loadingEndListener mLoadingEndListener;//完成之后的回调

    public LoadingButton(Context context) {
        this(context, null);
    }

    public LoadingButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LoadingButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        int bgColor, textColor, circleColor;

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.LoadingButton);
        try {
            bgColor = typedArray.getColor(R.styleable.LoadingButton_lbtn_backgroundColor, Color.BLUE);
            textColor = typedArray.getColor(R.styleable.LoadingButton_lbtn_textColor, Color.WHITE);
            circleColor = typedArray.getColor(R.styleable.LoadingButton_lbtn_loadingCircleColor, Color.WHITE);
            mTextSize = typedArray.getDimensionPixelSize(R.styleable.LoadingButton_lbtn_textSize, 40);
            String textContent = typedArray.getString(R.styleable.LoadingButton_lbtn_text);
            mTextContent = TextUtils.isEmpty(textContent) ? "CSnowStack" : textContent;
            loadingFailedReset = typedArray.getBoolean(R.styleable.LoadingButton_lbtn_loadingFailedReset, true);
            loadingSuccessRest = typedArray.getBoolean(R.styleable.LoadingButton_lbtn_loadingSuccessReset, false);
        } finally {
            typedArray.recycle();
        }


        //画背景
        mPaintBg = new Paint();
        mPaintBg.setColor(bgColor);
        mPaintBg.setAntiAlias(true);
        mPaintBg.setStyle(Paint.Style.FILL);

        //文字
        mPaintText = new Paint();
        mPaintText.setTextSize(mTextSize);
        mPaintText.setColor(textColor);
        mPaintText.setAntiAlias(true);
        mPaintText.setStyle(Paint.Style.FILL);
        mPaintText.setStrokeWidth(0);

        //中间的圆形进度效果,画勾,画叉
        mPaintProgress = new Paint();
        mPaintProgress.setColor(circleColor);
        mPaintProgress.setAntiAlias(true);
        mPaintProgress.setStyle(Paint.Style.STROKE);
        mPaintProgress.setStrokeWidth(5);
        mPathMeasure = new PathMeasure();

        //圆弧消失
        mEndCirclePaint = new Paint();
        mEndCirclePaint.setColor(circleColor);
        mEndCirclePaint.setAntiAlias(true);
        mEndCirclePaint.setStyle(Paint.Style.STROKE);
        mEndCirclePaint.setStrokeWidth(5);

        mPath = new Path();
        mDst = new Path();
        mTickPath = new Path();
        mTickDst = new Path();
        mCrossPath = new Path();
        mCrossDst = new Path();


        twoDecimalFormat = new DecimalFormat("#.00");
        oneDecimalFormat = new DecimalFormat("#.0");

    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int minHeight = mTextSize * 3 / 2;
        int minWidth = (int) mPaintText.measureText(mTextContent) * 3 / 2;
        if (heightMode == MeasureSpec.AT_MOST && widthMode == MeasureSpec.AT_MOST) {//都为包裹则都取最小
            width = minWidth;
            height = minHeight;
        } else if (heightMode == MeasureSpec.AT_MOST) {//高度包裹
            height = minHeight;
            if (width < minWidth)
                width = minWidth;
        } else if(widthMode == MeasureSpec.AT_MOST){//宽度包裹
            width = minWidth;
            if (height < minHeight)
                height = minHeight;

        }

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);


        mWidth = getMeasuredWidth();
        mHeight = getMeasuredHeight();
        mRadius = Math.min((mHeight - getPaddingTop() - getPaddingBottom()), (mWidth - getPaddingLeft() - getPaddingRight())) / 2;//半径,圆心y坐标
        mCircleProgressRadius = mRadius * 2 / 3;//loading所在小圆的半径
        mCenterX = (mWidth - getPaddingLeft() - getPaddingRight()) / 2;//圆心x坐标
        int centerY = (mHeight - getPaddingTop() - getPaddingBottom()) / 2;//圆心Y坐标
        //画loading
        mPath.addCircle(mCenterX, centerY, mCircleProgressRadius, Path.Direction.CW);
        mPathMeasure.setPath(mPath, true);
        mLength = mPathMeasure.getLength();

        int mStartPointX = mCenterX - mCircleProgressRadius;//loading之后新状态条开始的点
        //勾
        mTickMoveDistance = mCircleProgressRadius * 6 / 15;//向右移动的距离
        mTickPath.moveTo(mStartPointX, centerY);//到起始点
        mTickPath.lineTo(mStartPointX + mTickMoveDistance, centerY);//向右移动一点
        mTickPath.lineTo(mStartPointX + mTickMoveDistance + mCircleProgressRadius / 3, centerY + mCircleProgressRadius / 3);//向右下
        mTickPath.lineTo(mStartPointX + mTickMoveDistance + mCircleProgressRadius / 3 + mCircleProgressRadius, centerY - mCircleProgressRadius * 2 / 3);//向右上

        mTickPathMeasure = new PathMeasure(mTickPath, false);
        mTickLength = mTickPathMeasure.getLength();

        //叉,每个分支的长度为 2/3 mCircleProgressRadius,moveTo不行,drawLine的话多余的横线处理有点麻烦
        float crossLength = mCircleProgressRadius * 2f / 3f;
        mCrossPath.moveTo(mStartPointX, centerY);//到起始点
        mCrossPath.lineTo(mCenterX, centerY);//位移到中间准备画叉
        mCrossPath.lineTo(mCenterX - crossLength, centerY - crossLength);//左上
        mCrossPath.lineTo(mCenterX + crossLength, centerY + crossLength);//右下
        mCrossPath.lineTo(mCenterX, centerY);//到起始点
        mCrossPath.lineTo(mCenterX + crossLength, centerY - crossLength);//右上
        mCrossPath.lineTo(mCenterX - crossLength, centerY + crossLength);//左下

        mCrossPathMeasure = new PathMeasure(mCrossPath, false);
        mCrossLength = mCrossPathMeasure.getLength();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBG(canvas);
        if (mCurrentState == STATE_NORMAL && mPaintText.getAlpha() != 0) {  //写文本
            drawTxt(canvas);
        } else if (mCurrentState == STATE_CIRCLE_PROGRESS) {
            drawCircleLoading(canvas);

            //在左边中间的点结束转圈
            if ((isLoadingSuccess || isLoadingFailed) && twoDecimalFormat.format(mCircleAnimatorValue).equals(".50")) {
                changeLoadingEndState();
            }

        } else if (mCurrentState == STATE_LOADING_END) {
            drawDisappearCircle(canvas);

            if (isLoadingSuccess) {
                drawTick(canvas);
                return;
            }

            if (isLoadingFailed) {
                drawCross(canvas);
            }
        }

    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(MotionEvent.ACTION_DOWN==event.getAction()){
            if (!isLoading) {
                startMoveAnimation();
                isLoading = true;
            }
        }
        return super.onTouchEvent(event);
    }
    //画叉
    private void drawCross(Canvas canvas) {
        mCrossDst.reset();
        mCrossDst.lineTo(0, 0);
        float crossStart = 0;
        if (mStartRemoveSurplusLine) {
            crossStart = (mCircleProgressRadius) * ((mRemoveSurplusLineAnimatorValue - mCrossAnimatorValue) / mRemoveSurplusLineAnimatorValue);
        }
        mCrossPathMeasure.getSegment(crossStart, mCrossLength, mCrossDst, true);
        canvas.drawPath(mCrossDst, mPaintProgress);
    }

    //画勾
    private void drawTick(Canvas canvas) {
        mTickDst.reset();
        mTickDst.lineTo(0, 0);
        float tickStart = 0;
        if (mStartRemoveSurplusLine) {
            tickStart = (mTickMoveDistance) * ((mRemoveSurplusLineAnimatorValue - mTickAnimatorValue) / mRemoveSurplusLineAnimatorValue);
        }
        mTickPathMeasure.getSegment(tickStart, mTickLength, mTickDst, true);
        canvas.drawPath(mTickDst, mPaintProgress);
    }

    //画变短的圆弧
    private void drawDisappearCircle(Canvas canvas) {
        mDst.reset();
        mDst.lineTo(0, 0);
        float start = mLength / 2f * (1f - mEndCircleAnimatorValue);
        mPathMeasure.getSegment(start, mLength / 2f, mDst, true);
        canvas.drawPath(mDst, mEndCirclePaint);
    }

    //改变成为loading完成的状态
    private void changeLoadingEndState() {
        mCurrentState = STATE_LOADING_END;
        mCircleProgressAnimator.cancel();
        if (isLoadingSuccess) {
            startTickAnimation();
        } else {
            startCrossAnimation();
        }
    }

    //绘制中间loading
    private void drawCircleLoading(Canvas canvas) {
        mDst.reset();
        mDst.lineTo(0, 0);
        float stop = mLength * mCircleAnimatorValue;
        float start = (float) (stop - ((0.5 - Math.abs(mCircleAnimatorValue - 0.5)) * mLength));
        mPathMeasure.getSegment(start, stop, mDst, true);
        canvas.drawPath(mDst, mPaintProgress);
    }

    //绘制居中的文字
    private void drawTxt(Canvas canvas) {
        Rect bounds = new Rect();
        mPaintText.getTextBounds(mTextContent, 0, mTextContent.length(), bounds);
        Paint.FontMetricsInt fontMetrics = mPaintText.getFontMetricsInt();
        int baseLine = (mHeight / 2 + (fontMetrics.descent - fontMetrics.ascent) / 2 - fontMetrics.descent);
        canvas.drawText(mTextContent, mWidth/2- bounds.width() / 2, baseLine, mPaintText);
    }

    //画两边椭圆和中间的矩形背景
    private void drawBG(Canvas canvas) {
        canvas.drawArc(new RectF(mMoveWidth, 0, 2 * mRadius + mMoveWidth, mHeight), 90, 180, false, mPaintBg);
        canvas.drawRect(new RectF(mRadius + mMoveWidth, 0, mWidth - mRadius - mMoveWidth, mHeight), mPaintBg);
        canvas.drawArc(new RectF(mWidth - 2 * mRadius - mMoveWidth, 0, mWidth - mMoveWidth, mHeight), 180, 270, false, mPaintBg);
    }

    //两边向中间移动的动画
    private void startMoveAnimation() {
        //宽度
        ValueAnimator moveAnimator = ValueAnimator.ofInt(0, mCenterX - mRadius);
        ValueAnimator alphaAnimator = ValueAnimator.ofFloat(1, 0);
        moveAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mMoveWidth = (Integer) valueAnimator.getAnimatedValue();
                invalidate();
            }
        });

        //设置透明度
        alphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mPaintText.setAlpha(Math.round((Float) animation.getAnimatedValue()) * 255);
                mPaintText.setTextSize(mTextSize*((Float)animation.getAnimatedValue()));
            }
        });
        //结束的监听
        moveAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mCurrentState = STATE_CIRCLE_PROGRESS;
                startCircleProgressAnimation();
            }
        });
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(moveAnimator, alphaAnimator);
        animatorSet.setDuration(DURATION_NARROW).start();
    }

    //开始中间的loading动画
    private void startCircleProgressAnimation() {
        mCircleProgressAnimator = ValueAnimator.ofFloat(0, 1);
        mCircleProgressAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mCircleAnimatorValue = (float) valueAnimator.getAnimatedValue();
                invalidate();
            }
        });
        mCircleProgressAnimator.setDuration(DURATION_LOADING);
        mCircleProgressAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mCircleProgressAnimator.start();
    }

    //画对号的动画
    private void startTickAnimation() {
        ValueAnimator animator = ValueAnimator.ofFloat(1, 0);
        animator.setDuration(DURATION_END);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mTickAnimatorValue = (float) valueAnimator.getAnimatedValue();
                mEffect = new DashPathEffect(new float[]{mTickLength, mTickLength}, mTickAnimatorValue * mTickLength);
                mPaintProgress.setPathEffect(mEffect);

                if (!mStartRemoveSurplusLine) {
                    if (oneDecimalFormat.format(mTickAnimatorValue).equals(oneDecimalFormat.format(mTickMoveDistance / mTickLength))) {
                        mStartRemoveSurplusLine = true;
                        mRemoveSurplusLineAnimatorValue = mTickAnimatorValue;
                    }
                }

                invalidate();
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (loadingSuccessRest) {
                    reset(true);
                } else {
                    if (mLoadingEndListener != null)
                        mLoadingEndListener.onLoadingEndListener(true);
                }


            }
        });


        ValueAnimator endAnimator = ValueAnimator.ofFloat(1, 0);
        endAnimator.setDuration((long) (DURATION_END * (1 - (mTickMoveDistance) / mTickLength)));
        endAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mEndCircleAnimatorValue = (float) valueAnimator.getAnimatedValue();
            }
        });
        animator.start();
        endAnimator.start();
    }

    //开始画叉的动画
    private void startCrossAnimation() {
        ValueAnimator animator = ValueAnimator.ofFloat(1, 0);
        animator.setDuration(DURATION_END);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mCrossAnimatorValue = (float) valueAnimator.getAnimatedValue();
                mEffect = new DashPathEffect(new float[]{mCrossLength, mCrossLength}, mCrossAnimatorValue * mCrossLength);
                mPaintProgress.setPathEffect(mEffect);
                DecimalFormat df = new DecimalFormat("#.0");
                if (!mStartRemoveSurplusLine) {
                    if (df.format(mCrossAnimatorValue).equals(df.format((mCircleProgressRadius) / mCrossLength))) {
                        mStartRemoveSurplusLine = true;
                        mRemoveSurplusLineAnimatorValue = mCrossAnimatorValue;
                    }
                }

                invalidate();
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);

                if (loadingFailedReset) {
                    reset(false);
                } else {
                    if (mLoadingEndListener != null)
                        mLoadingEndListener.onLoadingEndListener(false);
                }


            }
        });


        ValueAnimator endAnimator = ValueAnimator.ofFloat(1, 0);
        endAnimator.setDuration((long) (DURATION_END * (1 - (mCircleProgressRadius) / mCrossLength)));
        endAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mEndCircleAnimatorValue = (float) valueAnimator.getAnimatedValue();
            }
        });
        animator.start();
        endAnimator.start();
    }

    private void reset(final boolean loadSuccess) {
        mCurrentState = STATE_NORMAL;
        ValueAnimator moveAnimator = ValueAnimator.ofInt(mCenterX - mRadius, 0);
        ValueAnimator alphaAnimator = ValueAnimator.ofFloat(0, 1);
        moveAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mMoveWidth = (Integer) valueAnimator.getAnimatedValue();
                invalidate();
            }
        });

        //设置透明度
        alphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mPaintText.setAlpha(Math.round((Float) animation.getAnimatedValue()) * 255);
                mPaintText.setTextSize(mTextSize*((Float)animation.getAnimatedValue()));
            }
        });
        //结束的监听
        moveAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (mLoadingEndListener != null)
                    mLoadingEndListener.onLoadingEndListener(loadSuccess);
                isLoadingSuccess = isLoadingFailed = isLoading = false;
            }
        });
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(moveAnimator, alphaAnimator);
        animatorSet.setDuration(DURATION_RESET).start();

    }

    //标记加载失败
    public void setLoadingFailed() {
        isLoadingFailed = true;
    }

    //标记加载成功
    public void setLoadingSuccess() {
        isLoadingSuccess = true;
    }

    public static interface loadingEndListener {
        void onLoadingEndListener(boolean loadingSuccess);
    }

    public void setLoadingEndListener(loadingEndListener loadingEndListener) {
        mLoadingEndListener = loadingEndListener;
    }
}
