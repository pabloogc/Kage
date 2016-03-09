package com.bq.kage

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES20.*
import android.opengl.GLUtils
import android.util.Log
import java.nio.FloatBuffer
import java.nio.IntBuffer

const val GRID_ROWS = 16 * 10
const val GRID_COLUMNS = 9 * 10
const val MODE = GL_TRIANGLES

class Page(context: Context, val width: Float, val height: Float) {

    private val program: Program

    private val positionAttr: Int
    private val colorAttr: Int
    private val textureAttr: Int

    private val textureUniform: Int
    private val apexUniform: Int
    private val boundsUniform: Int
    private val mvpUniform: Int
    private val directionUniform: Int

    private val vertexPosition: FloatArray
    private val vertexPositionBuffer: FloatBuffer

    private val texturePosition: FloatArray
    private val texturePositionBuffer: FloatBuffer

    private val vertexColor: FloatArray
    private val vertexColorBuffer: FloatBuffer

    private val vertexDrawOrder: IntArray
    private val vertexDrawOrderBuffer: IntBuffer

    private val apex = floatArrayOf(0.25f, -0.25f)

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
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0).glCheck()

        bitmap.recycle()

        //Attributes

        positionAttr = program.getAttribLocation("position").glCheck()
        colorAttr = program.getAttribLocation("color").glCheck()
        textureAttr = program.getAttribLocation("textCoord").glCheck()


        boundsUniform = program.getUniformLocation("bounds").glCheck()
        textureUniform = program.getUniformLocation("textureSampler").glCheck()
        apexUniform = program.getUniformLocation("apex").glCheck()
        directionUniform = program.getUniformLocation("direction").glCheck()
        mvpUniform = program.getUniformLocation("mvp").glCheck()

    }

    fun draw(mvpMatrix: FloatArray, x: Float, y: Float) {
        program.enable()


        //        val deltaY = 1f * (if (y != 0f) Math.signum(y) else 1f);
        val s = (if ( y > 0) -1 else 1)
        val deltaY = s * 0.25f;
        var dx = s * (foldLineFunction(y - deltaY) - foldLineFunction(y + deltaY));
        var dy = s * (y - deltaY)
        val mod = Math.sqrt(dx.toDouble() * dx + dy * dy);
        dx /= mod.toFloat();
        dy /= mod.toFloat();

        apex[0] = -width / 2
        apex[1] = (dy / dx) * (apex[0] - x) + y;
        Log.d("Kage", "x:$x y:$y (${apex[0]}, ${apex[1]})")

//        if (apex[1] < height / 2 && apex[1] > 0) apex[1] = height / 2
//        if (apex[1] > -height / 2 && apex[1] < 0) apex[1] = -height / 2

        //        apex[1] = Math.min(-height / 2, apex[1]);
        //        apex[1] = Math.min(height / 2, apex[1]);


        //        dx = Math.sqrt(2.0).toFloat();
        //        dy = dx;

        val sqrt2 = x * Math.sqrt(2.0).toFloat()


        glUniform2fv(apexUniform, 1, apex, 0).glCheck()
        glUniform2fv(directionUniform, 1, floatArrayOf(dx, dy), 0).glCheck()
        glUniform4fv(boundsUniform, 1, floatArrayOf(
                -width / 2f,
                height / 2f,
                width / 2f,
                -height / 2f), 0).glCheck()

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

        glDrawElements(MODE,
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
        val w = width / (GRID_COLUMNS - 1)
        val h = height / (GRID_ROWS - 1)

        val cx = -width / 2f;
        val cy = -height / 2f;

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

                val r = 1f
                val g = 1f
                val b = 1f

                colors[p + 0] = r
                colors[p + 1] = g
                colors[p + 2] = b
                colors[p + 3] = 1.0f //unused alpha, there is no blend
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
        val w = 1.0f / (GRID_COLUMNS - 1)
        val h = 1.0f / (GRID_ROWS - 1)
        val textureMap = FloatArray((vertexPosition.size / 3) * 2) //map 1 texture to each vertex
        for (i in 0..GRID_ROWS - 1) {
            for (j in 0..GRID_COLUMNS - 1) {
                val p = 2 * (i * GRID_COLUMNS + j)
                val s = j * w;
                val t = i * h;
                textureMap[p + 0] = s
                textureMap[p + 1] = t
            }
        }
        return textureMap
                .mapIndexed { i, v -> if (i % 2 == 1) -v else v } //Flip y
                .toFloatArray()
    }

    private fun foldLineFunction(y: Float): Float {
        return -0.3f * (y * y);
    }
}