package com.bq.kage

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


class KageRenderer(val context: Context) : GLSurfaceView.Renderer {

  lateinit var page: Page

  override fun onSurfaceCreated(unused: GL10, eglConfig: EGLConfig) {
    page = Page(context)
  }

  override fun onSurfaceChanged(unused: GL10, w: Int, h: Int) {
    GLES20.glViewport(0, 0, w, h)
  }

  override fun onDrawFrame(unused: GL10) {
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
    GLES20.glClearColor(1f, 0f, 0f, 1f)
    page.draw()
  }
}