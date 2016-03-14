package com.bq.kage

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.opengl.GLES20
import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

const val USE_3D: Boolean = false;

class KageView(context: Context?, attrs: AttributeSet?)
: GLSurfaceView(context, attrs) {

    var animator : ObjectAnimator? = null
    var animating = false
    var ratio = 0f;

    val ctx: Context

    var touchX = 1f
        set(value) {
            field = value
            requestRender()
        }

    var touchY = 0f
        set(value) {
            field = value
            requestRender()
        }

    init {
        ctx = context!!
    }

    public fun init() {
        setEGLContextClientVersion(2);
        setEGLConfigChooser(MultisampleConfigChooser())
        setRenderer(kageRenderer)
        renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        //map to -1, 1
        if (animating) {
            return true
        };

        val x = (event.x / width / 0.5f) - 1;
        val y = -(event.y / height / 0.5f) + 1;

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                animating = true
                val xProp = PropertyValuesHolder.ofFloat("touchX", x)
                val yProp = PropertyValuesHolder.ofFloat("touchY", y)

                animator = ObjectAnimator.ofPropertyValuesHolder(this, xProp, yProp).apply {
                    duration = 400
//                    interpolator = DecelerateInterpolator()
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(anim: Animator) {
                            animating = false
                        }
                    })
                    start()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_MOVE -> {
                touchX = x
                touchY = y
            }
        }

        if (event.action == MotionEvent.ACTION_DOWN) {

        } else {

        }

        requestRender()
        return true
    }

    private val kageRenderer = object : GLSurfaceView.Renderer {
        private val modelMatrix = FloatArray(16)
        private val viewMatrix = FloatArray(16)
        private val projectionMatrix = FloatArray(16)
        private val mvpMatrix = FloatArray(16)

        lateinit var page: Page

        override fun onSurfaceCreated(unused: GL10, eglConfig: EGLConfig) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.setIdentityM(viewMatrix, 0)
            Matrix.setIdentityM(projectionMatrix, 0)
            Matrix.setIdentityM(mvpMatrix, 0)

            glEnable (GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LEQUAL);
        }

        override fun onSurfaceChanged(unused: GL10, w: Int, h: Int) {
            glViewport(0, 0, w, h)

            Matrix.setIdentityM(projectionMatrix, 0)
            Matrix.setIdentityM(mvpMatrix, 0)
            Matrix.setIdentityM(modelMatrix, 0)

            ratio = h.toFloat() / w;
            val left = -1f;
            val right = 1f;
            val bottom = -ratio;
            val top = ratio;
            val near = 1.0f;
            val far = 10.0f;

            if (USE_3D) {
                Matrix.frustumM(projectionMatrix, 0, left, right, bottom, top, near, far);
            } else {
                Matrix.orthoM(projectionMatrix, 0, left, right, bottom, top, -10f, 10f);
            }

            val eyeX = 0.0f;
            val eyeY = 0.0f;
            val eyeZ = 1.0f;

            val lookX = 0.0f;
            val lookY = 0.0f;
            val lookZ = 0.0f;

            val upX = 0.0f;
            val upY = 1.0f;
            val upZ = 0.0f;

            Matrix.setLookAtM(viewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ)

            page = Page(ctx, 1f, 1f * ratio)
        }

        override fun onDrawFrame(unused: GL10) {
            glClear(GL_COLOR_BUFFER_BIT)
            glClear(GL_DEPTH_BUFFER_BIT);

            val period = 100000
            val rot = (System.currentTimeMillis() % period) / period.toFloat() * 360

            glClearColor(1f, 1f, 1f, 1f)

            if (USE_3D) {
                //Top left
                Matrix.translateM(modelMatrix, 0, -0.5f, ratio / 2, -0.5f)
                drawAndReset()

                //Top right
                Matrix.translateM(modelMatrix, 0, 0.5f, ratio / 2, -0.5f)
                Matrix.rotateM(modelMatrix, 0, -90f, 0f, 1f, 0f)
                drawAndReset()

                //Bottom left
                Matrix.translateM(modelMatrix, 0, -0.5f, -ratio / 2, -0.5f)
                Matrix.rotateM(modelMatrix, 0, -90f, 1f, 0f, 0f)
                drawAndReset()

                //Bottom right
                Matrix.translateM(modelMatrix, 0, 0.5f, -ratio / 2, -0.5f)
                Matrix.rotateM(modelMatrix, 0, 90f, 1f, 0f, 0f)
                drawAndReset()

            } else {
                Matrix.scaleM(modelMatrix, 0, 2f, 2f, 1f)
                drawAndReset()
            }
        }

        fun drawAndReset() {
            Matrix.setIdentityM(mvpMatrix, 0)

            Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)
            page.draw(mvpMatrix, touchX, touchY)

            Matrix.setIdentityM(mvpMatrix, 0)
            Matrix.setIdentityM(modelMatrix, 0)
        }
    }


}

