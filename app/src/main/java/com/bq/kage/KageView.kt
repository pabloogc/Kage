package com.bq.kage

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

const val USE_3D: Boolean = false;

class KageView(context: Context?, attrs: AttributeSet?)
: GLSurfaceView(context, attrs) {


    val c: Context
    var touchX = 0f
    var touchY = 0f

    init {
        c = context!!
    }

    public fun init() {
        setEGLContextClientVersion(2);
        setRenderer(r)
        renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        touchX = -1 + 2 * (event.x / width)
        touchY = -(-1 + 2 * (event.y / height))
        //Mote to model coordinates
        requestRender()
        return true
    }

    private val r = object : GLSurfaceView.Renderer {
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
        }

        override fun onSurfaceChanged(unused: GL10, w: Int, h: Int) {
            GLES20.glViewport(0, 0, w, h)

            Matrix.setIdentityM(projectionMatrix, 0)
            val ratio = w.toFloat() / h;
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

            page = Page(c, 1f, ratio.toFloat())
        }

        override fun onDrawFrame(unused: GL10) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);

            GLES20.glClearColor(0f, 0f, 0f, 0f)

            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            GLES20.glDepthFunc(GLES20.GL_LEQUAL);

            val period = 100000
            val rot = (System.currentTimeMillis() % period) / period.toFloat() * 360

            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.setIdentityM(mvpMatrix, 0)
            //        Matrix.scaleM(modelMatrix, 0, 1f, -1f, 1f)
            if (USE_3D) {
                Matrix.translateM(modelMatrix, 0, 0f, 0f, -1.5f)
                Matrix.translateM(modelMatrix, 0, -1f, 0f, 0f)
                Matrix.rotateM(modelMatrix, 0, -25f, 1f, 1f, 0f)
            }
            Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

            page.draw(mvpMatrix, touchX, touchY)

            if (USE_3D) {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.setIdentityM(mvpMatrix, 0)

                //Matrix.scaleM(modelMatrix, 0, -1f, 1f, 1f) //Flip Horizontal
                Matrix.translateM(modelMatrix, 0, 1f, 0f, -1.5f)
                Matrix.rotateM(modelMatrix, 0, 25f, 1f, 1f, 0f)
                Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
                Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

                page.draw(mvpMatrix, touchX, touchY)
            }
        }
    }
}

