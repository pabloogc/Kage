package com.bq.kage

import android.content.Context
import android.opengl.GLES20.*
import java.nio.FloatBuffer
import java.nio.IntBuffer

const val POSITION_ATTR = 1
const val COLOR_ATTR = 2

const val COORDS_PER_VERTEX = 3
const val FLOAT_BYTE_COUNT = 4
const val VERTEX_STRIDE = COORDS_PER_VERTEX * FLOAT_BYTE_COUNT

const val GRID_ROWS = 12
const val GRID_COLUMNS = 12

class Page(context: Context) {

    private val program: Program

    private val vertexPosition: FloatArray
    private val vertexPositionBuffer: FloatBuffer

    private val vertexColor: FloatArray
    private val vertexColorBuffer: FloatBuffer

    private val gridDrawOrder: IntArray
    private val gridDrawOrderBuffer: IntBuffer

    private val buffers = IntArray(2)


    init {
        vertexPosition = calculateVertexPositions()
        vertexPositionBuffer = vertexPosition.toBuffer()

        vertexColor = calculateVertexColor()
        vertexColorBuffer = vertexColor.toBuffer()

        gridDrawOrder = calculateVertexDrawOrder()
        gridDrawOrderBuffer = gridDrawOrder.toBuffer()

        program = program {
            shader {
                attr("position", POSITION_ATTR)
                attr("color", COLOR_ATTR)
                asset(context, "vertex_shader.vert", GL_VERTEX_SHADER)
            }
            shader {
                asset(context, "fragment_shader.frag", GL_FRAGMENT_SHADER)
            }
        }
    }

    fun draw() {
        program.enable()

        // get handle to vertex shader's vPosition member

        glEnableVertexAttribArray(COLOR_ATTR).glCheck()
        glVertexAttribPointer(COLOR_ATTR,
                4, GL_FLOAT, false,
                4 * 4, vertexColorBuffer)


        glEnableVertexAttribArray(POSITION_ATTR).glCheck()
        glVertexAttribPointer(POSITION_ATTR,
                3, GL_FLOAT, false,
                4 * 3, vertexPositionBuffer);

        //Draw the geometric data using the draw order
        glDrawElements(GL_POINTS,
                gridDrawOrder.size,
                GL_UNSIGNED_INT,
                gridDrawOrderBuffer);


        program.disableAttrib(POSITION_ATTR)
        program.disableAttrib(COLOR_ATTR)

        program.disable()
    }


    private fun calculateVertexPositions(): FloatArray {
        val w = 2f / (GRID_COLUMNS - 1)
        val h = 2f / (GRID_ROWS - 1)
        val g = FloatArray(3 * GRID_ROWS * GRID_COLUMNS)
        for (i in 0..GRID_ROWS - 1) {
            for (j in 0..GRID_COLUMNS - 1) {
                val p = 3 * (i * GRID_COLUMNS + j)
                g[p] = -1f + j * w//x
                g[p + 1] = 1f - i * h //y
                g[p + 2] = 0f //z
            }
        }
        return g
    }

    private fun calculateVertexColor(): FloatArray {
        val colors = FloatArray(4 * GRID_ROWS * GRID_COLUMNS)
        for (i in 0..GRID_ROWS - 1) {
            for (j in 0..GRID_COLUMNS - 1) {
                val p = 4 * (i * GRID_COLUMNS + j)

                var r = 0f
                var g = 0f
                var b = 0f

                if (i % 2 == 0) {
                    r = 1f;
                } else {
                    g = 1f
                }

                if (j % 2 == 0) {
                    b = 1f
                } else {
                    r = 1f
                    b = 1f
                }

                colors[0] = 1f
                colors[1] = 1f
                colors[2] = 1f
                colors[3] = 1f //alpha
            }
        }
        return colors
    }

    private fun calculateVertexDrawOrder(): IntArray {
        val squares = (GRID_ROWS - 1) * (GRID_COLUMNS - 1)
        val o = kotlin.IntArray(squares * 6) //3 per triangle in the square

        for (i in 0..GRID_ROWS - 2) {
            for (j in 0..GRID_COLUMNS - 2) {
                val p = i * GRID_COLUMNS + j

                val v0 = p
                val v1 = v0 + 1
                val v2 = (i + 1) * GRID_COLUMNS + j
                val v3 = v2 + 1

                val idx = (i * (GRID_COLUMNS - 1) + j) * 6
                o[idx + 0] = v0 //Bottom triangle
                o[idx + 1] = v2
                o[idx + 2] = v3
                o[idx + 3] = v0 //Top triangle
                o[idx + 4] = v3
                o[idx + 5] = v1
            }
        }
        return o;
    }
}