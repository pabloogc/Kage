package com.bq.kage

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES20.*
import android.opengl.GLUtils
import java.nio.FloatBuffer
import java.nio.IntBuffer

const val GRID_WIDTH = 2f
const val GRID_HEIGHT = 2f

const val GRID_ROWS = 160
const val GRID_COLUMNS = 90

class Page(context: Context) {

    private val program: Program

    private val positionAttr: Int;
    private val colorAttr: Int;
    private val textureAttr: Int;

    private val textureUniform: Int;
    private val touchUniform: Int;
    private val mvpUniform: Int;

    private val vertexPosition: FloatArray
    private val vertexPositionBuffer: FloatBuffer

    private val texturePosition: FloatArray
    private val texturePositionBuffer: FloatBuffer

    private val vertexColor: FloatArray
    private val vertexColorBuffer: FloatBuffer

    private val vertexDrawOrder: IntArray
    private val vertexDrawOrderBuffer: IntBuffer

    private val touchPosition = FloatArray(2, { -10f })

    //Buffers
    private val buffers = IntArray(4)
    private val textures = IntArray(1)


    init {
        vertexPosition = calculateVertexPositions()
        vertexPositionBuffer = vertexPosition.toBuffer()

        vertexColor = calculateVertexColor()
        vertexColorBuffer = vertexColor.toBuffer()

        vertexDrawOrder = calculateVertexDrawOrder()
        vertexDrawOrderBuffer = vertexDrawOrder.toBuffer()

        texturePosition = calculateTextureMapping()
        texturePositionBuffer = texturePosition.toBuffer()

        program = program {
            shader {
                asset(context, "vertex_shader.vert", GL_VERTEX_SHADER)
            }
            shader {
                asset(context, "fragment_shader.frag", GL_FRAGMENT_SHADER)
            }
        }


        //Geometry
        glGenBuffers(buffers.size, buffers, 0).glCheck()

        //draw order
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, buffers[0]).glCheck()
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, vertexDrawOrder.size * 4, vertexDrawOrderBuffer, GL_STATIC_DRAW).glCheck()
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)

        //positions
        glBindBuffer(GL_ARRAY_BUFFER, buffers[1]).glCheck()
        glBufferData(GL_ARRAY_BUFFER, vertexPosition.size * 4, vertexPositionBuffer, GL_STATIC_DRAW).glCheck()
        glBindBuffer(GL_ARRAY_BUFFER, 0)

        //colors
        glBindBuffer(GL_ARRAY_BUFFER, buffers[2]).glCheck()
        glBufferData(GL_ARRAY_BUFFER, vertexColor.size * 4, vertexColorBuffer, GL_STATIC_DRAW).glCheck()
        glBindBuffer(GL_ARRAY_BUFFER, 0)

        //texture coordinates
        glBindBuffer(GL_ARRAY_BUFFER, buffers[3]).glCheck()
        glBufferData(GL_ARRAY_BUFFER, texturePosition.size * 4, texturePositionBuffer, GL_STATIC_DRAW).glCheck()
        glBindBuffer(GL_ARRAY_BUFFER, 0)

        //Textures
        val options = BitmapFactory.Options().apply {
            inScaled = false
        }

        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.texture, options)

        glGenTextures(textures.size, textures, 0)

        glBindTexture(GL_TEXTURE_2D, textures[0])
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0).glCheck()

        bitmap.recycle()

        //Attributes

        positionAttr = program.getAttribLocation("position").glCheck()
        colorAttr = program.getAttribLocation("color").glCheck()
        textureAttr = program.getAttribLocation("textCoord").glCheck()
        textureUniform = program.getUniformLocation("textureSampler").glCheck()
        touchUniform = program.getUniformLocation("touch").glCheck()
        mvpUniform = program.getUniformLocation("mvp").glCheck()
    }

    fun draw(mvpMatrix: FloatArray, x: Float, y: Float) {
        program.enable()

        touchPosition[0] = x
        touchPosition[1] = y
        glUniform2fv(touchUniform, 1, touchPosition, 0).glCheck()
        glUniformMatrix4fv(mvpUniform, 1, false, mvpMatrix, 0).glCheck()

        //Texture uniform
        glActiveTexture(GL_TEXTURE0).glCheck()
        glBindTexture(GL_TEXTURE_2D, textures[0]).glCheck()
        glUniform1i(textureUniform, 0).glCheck()


        glEnableVertexAttribArray(positionAttr).glCheck()
        glBindBuffer(GL_ARRAY_BUFFER, buffers[1]).glCheck()
        glVertexAttribPointer(positionAttr,
                3, GL_FLOAT, false,
                3 * 4, 0).glCheck();

        glEnableVertexAttribArray(colorAttr).glCheck()
        glBindBuffer(GL_ARRAY_BUFFER, buffers[2]).glCheck()
        glVertexAttribPointer(colorAttr,
                4, GL_FLOAT, false,
                4 * 4, 0).glCheck();

        glEnableVertexAttribArray(textureAttr).glCheck()
        glBindBuffer(GL_ARRAY_BUFFER, buffers[3]).glCheck()
        glVertexAttribPointer(textureAttr,
                2, GL_FLOAT, false,
                2 * 4, 0).glCheck()

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, buffers[0])

        glDrawElements(GL_TRIANGLES,
                vertexDrawOrder.size,
                GL_UNSIGNED_INT,
                0).glCheck();


        glDisableVertexAttribArray(positionAttr).glCheck()
        glDisableVertexAttribArray(colorAttr).glCheck()
        glDisableVertexAttribArray(textureAttr).glCheck()

        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)

        program.disable()
    }


    private fun calculateVertexPositions(): FloatArray {
        val w = GRID_WIDTH / (GRID_COLUMNS - 1)
        val h = GRID_HEIGHT / (GRID_ROWS - 1)

        val cx = -1
        val cy = -1

        val g = FloatArray(3 * GRID_ROWS * GRID_COLUMNS)
        for (i in 0..GRID_ROWS - 1) {
            for (j in 0..GRID_COLUMNS - 1) {
                val p = 3 * (i * GRID_COLUMNS + j)
                g[p] = cx + j * w//x
                g[p + 1] = cy + i * h //y
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

                val r = (i.toFloat() / GRID_ROWS)
                val g = (j.toFloat() / GRID_COLUMNS)
                val b = 1.0f

                colors[p + 0] = r
                colors[p + 1] = g
                colors[p + 2] = b
                colors[p + 3] = 1.0f //alpha
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

    private fun calculateTextureMapping(): FloatArray {
        return vertexPosition
                .filterIndexed { i, v ->
                    i % 3 != 2 //drop z
                }
                .mapIndexed { i, v -> if (i % 2 == 1) -v else v } //flip Y
                .map { (it + 1) / GRID_WIDTH }
                .toFloatArray()
    }
}