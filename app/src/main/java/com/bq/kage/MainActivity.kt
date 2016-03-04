package com.bq.kage

import android.opengl.GLSurfaceView
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*;

class MainActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    surfaceView.setEGLContextClientVersion(2);
    surfaceView.setRenderer(KageRenderer(this))
    surfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
  }
}
