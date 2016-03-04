package com.bq.kage

import android.content.Context
import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

const val COORDS_PER_VERTEX = 3
const val FLOAT_BYTE_COUNT = 4
const val VERTEX_STRIDE = COORDS_PER_VERTEX * FLOAT_BYTE_COUNT

const val GRID_ROWS = 1
const val GRID_COLUMNS = 1

class Page(context: Context) {

  private val program: Program

  private val vertexBuffer: FloatBuffer
  private val drawOrderBuffer: ShortBuffer


  private val gridCoords: FloatArray
  private val gridDrawOrder: ShortArray

  init {
    gridCoords = createGrid()
    gridDrawOrder = createGridDrawOrder()

    var bb = ByteBuffer.allocateDirect(
        gridCoords.size * 4 /*4 bytes per float*/);

    // use the device hardware's native byte order
    bb.order(ByteOrder.nativeOrder());

    // create a floating point buffer from the ByteBuffer
    vertexBuffer = bb.asFloatBuffer();
    vertexBuffer.put(gridCoords);
    vertexBuffer.position(0);

    bb = ByteBuffer.allocateDirect(gridDrawOrder.size * 2)
    bb.order(ByteOrder.nativeOrder())
    drawOrderBuffer = bb.asShortBuffer()
    drawOrderBuffer.put(gridDrawOrder)
    drawOrderBuffer.position(0)

    program = program {
      vertexShader(Shader.fromAsset(GLES20.GL_VERTEX_SHADER, context, "vertex_shader.vert"))
      fragmentShader(Shader.fromAsset(GLES20.GL_FRAGMENT_SHADER, context, "fragment_shader.frag"))
    }
  }

  fun draw() {
    program.use()
    // get handle to vertex shader's vPosition member
    val positionHandle = GLES20.glGetAttribLocation(program.program, "vPosition");

    // Enable a handle to the triangle vertices
    GLES20.glEnableVertexAttribArray(positionHandle);

    // Prepare the triangle coordinate data


    // get handle to fragment shader's vColor member
    val mColorHandle = GLES20.glGetUniformLocation(program.program, "vColor");

    // Set color for drawing the triangle
    GLES20.glUniform4fv(mColorHandle, 1, floatArrayOf(1f, 1f, 1f, 1f), 0);

    //Load geometric data
    GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX,
        GLES20.GL_FLOAT, false,
        VERTEX_STRIDE, vertexBuffer);

    //Draw the geometric data using the draw order
    GLES20.glDrawElements(GLES20.GL_LINES,
        gridDrawOrder.size,
        GLES20.GL_UNSIGNED_SHORT,
        drawOrderBuffer);

    // Disable vertex array
    GLES20.glDisableVertexAttribArray(positionHandle);
  }

  private fun createGrid(): FloatArray {
    return floatArrayOf(
        -0.5f, 0.5f, 0.0f, // top left
        -0.5f, -0.5f, 0.0f, // bottom left
        0.5f, -0.5f, 0.0f, // bottom right
        0.5f, 0.5f, 0.0f) ; // top right)
  }

  private fun createGridDrawOrder(): ShortArray {
    return shortArrayOf(0, 1, 2, 0, 2, 3)
  }

}