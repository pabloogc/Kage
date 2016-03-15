package com.bq.kage

import android.content.Context
import android.graphics.*
import android.opengl.GLES20.*
import android.opengl.GLUtils
import android.support.annotation.FloatRange
import android.util.Log
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.ShortBuffer

const val TAG = "Kage"
const val RAD = 0.15f
const val PI = 0.15f * Math.PI.toFloat()
const val DIAMETER = 2 * RAD;
const val MODE = GL_TRIANGLES
const val SHADOW_PADDING = 0

class Page(context: Context, val width: Float, val height: Float) {

    private val padding = if (SHADOW_PADDING == 0) 0f else 1f / SHADOW_PADDING
    private val gridRows: Int
    private val gridColumns: Int

    private val program: Program

    private val positionAttr: Int
    private val colorAttr: Int
    private val textureAttr: Int

    private val textureUniform: Int
    private val apexUniform: Int
    private val boundsUniform: Int
    private val mvpUniform: Int
    private val directionUniform: Int
    private val fingerTipUniform: Int

    private val vertexPosition: FloatArray
    private val vertexPositionBuffer: FloatBuffer

    private val texturePosition: FloatArray
    private val texturePositionBuffer: FloatBuffer

    private val vertexColor: FloatArray
    private val vertexColorBuffer: FloatBuffer

    private val vertexDrawOrder: ShortArray
    private val vertexDrawOrderBuffer: ShortBuffer

    private val apex = floatArrayOf(0.25f, -0.25f)

    //Buffers
    private val buffers = IntArray(4)
    private val textures = IntArray(1)


    init {

        gridRows = 16
        gridColumns = (gridRows * (height / width)).toInt()
        //        gridColumns = 180

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
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, vertexDrawOrder.size * 2, vertexDrawOrderBuffer, GL_STATIC_DRAW).glCheck()
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
            //inScaled = false
        }


        //Texture generation

        val baseBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.texture, options)
        val bitmap = Bitmap.createBitmap(
                baseBitmap.width + 2 * SHADOW_PADDING,
                baseBitmap.height + 2 * SHADOW_PADDING,
                Bitmap.Config.ARGB_8888);

        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK)
        canvas.drawBitmap(baseBitmap,
                SHADOW_PADDING.toFloat(),
                SHADOW_PADDING.toFloat(),
                Paint(Paint.FILTER_BITMAP_FLAG))

        baseBitmap.recycle()


        glGenTextures(textures.size, textures, 0)
        glBindTexture(GL_TEXTURE_2D, textures[0])
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0).glCheck()

        bitmap.recycle()

        //Attributes

        positionAttr = program.getAttribLocation("position").glCheck()
        colorAttr = program.getAttribLocation("color").glCheck()
        textureAttr = program.getAttribLocation("textCoord").glCheck()

        boundsUniform = program.getUniformLocation("bounds").glCheck()
        fingerTipUniform = program.getUniformLocation("finger_tip").glCheck()
        textureUniform = program.getUniformLocation("textureSampler").glCheck()
        apexUniform = program.getUniformLocation("apex").glCheck()
        directionUniform = program.getUniformLocation("direction").glCheck()
        mvpUniform = program.getUniformLocation("mvp").glCheck()

    }

    fun draw(mvpMatrix: FloatArray,
             @FloatRange(from = -1.0, to = 1.0) x: Float,
             @FloatRange(from = -1.0, to = 1.0) y: Float) {

        program.enable()

        //Map ranges to model dimensions
        var xt = 0.5f * x * width;
        var yt = 0.5f * y * height;

        val right = 0.5f;
        val turn_point = 0.1f

        if (xt < 0.0) { //Middle to left
            xt /= 2f;
        } else if (right - xt < turn_point) { //Right edge, page rising
            val p = (right - xt) / turn_point;
            xt = right - p * turn_point * 3f ;
        } else { //curve starts
            val p = 0.5f * xt / (2 * turn_point)
            xt = 2f * turn_point * p;
        }

        //Derivative of the function to figure out the inclination
        val sigNum = (if (yt > 0) -1 else 1)
        val deltaY = sigNum * 0.25f;
        var dx = sigNum * (foldLineFunction(yt - deltaY) - foldLineFunction(yt + deltaY));
        var dy = sigNum * (yt - 0.5f * deltaY)

        //Normalize
        val mod = Math.sqrt(dx.toDouble() * dx + dy * dy);
        dx /= mod.toFloat();
        dy /= mod.toFloat();

        //Intersection between the edge of the page and the cilinder
        apex[0] = -width / 2;
        apex[1] = (dy / dx) * (apex[0] - xt) + yt;

        glUniform2fv(fingerTipUniform, 1, floatArrayOf(xt, yt), 0).glCheck()
        glUniform2fv(apexUniform, 1, apex, 0).glCheck()
        glUniform2fv(directionUniform, 1, floatArrayOf(dx, dy), 0).glCheck()
        glUniform1fv(boundsUniform, 4, floatArrayOf(
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

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, buffers[0]).glCheck()
        glDrawElements(MODE,
                vertexDrawOrder.size,
                GL_UNSIGNED_SHORT,
                0).glCheck();


        glDisableVertexAttribArray(positionAttr).glCheck()
        glDisableVertexAttribArray(colorAttr).glCheck()
        glDisableVertexAttribArray(textureAttr).glCheck()

        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)

        program.disable()
    }


    private fun calculateVertexPositions(): FloatArray {
        val w = (width + 2 * padding) / (gridColumns - 1)
        val h = (height + 2 * padding) / (gridRows - 1)

        val cx = -width / 2f - padding;
        val cy = -height / 2f - padding;

        val g = FloatArray(3 * gridRows * gridColumns)
        for (i in 0..gridRows - 1) {
            for (j in 0..gridColumns - 1) {
                val p = 3 * (i * gridColumns + j)
                g[p] = cx + j * w//x
                g[p + 1] = cy + i * h //y
                g[p + 2] = 0f //z
            }
        }
        return g
    }

    private fun calculateVertexColor(): FloatArray {
        val colors = FloatArray(4 * gridRows * gridColumns)
        for (i in 0..gridRows - 1) {
            for (j in 0..gridColumns - 1) {
                val p = 4 * (i * gridColumns + j)

                val r = 1f
                val g = 1f
                val b = 1f

                colors[p + 0] = r
                colors[p + 1] = g
                colors[p + 2] = b
                colors[p + 3] = 1f
            }
        }
        return colors
    }

    private fun calculateVertexDrawOrder(): ShortArray {
        val squares = (gridRows - 1) * (gridColumns - 1)
        val o = kotlin.ShortArray(squares * 6) //3 per triangle in the square

        for (i in gridRows - 2 downTo 0) {
            for (j in gridColumns - 2 downTo 0) {
                val p = i * gridColumns + j

                val v0 = p
                val v1 = v0 + 1
                val v2 = (i + 1) * gridColumns + j
                val v3 = v2 + 1

                val idx = (i * (gridColumns - 1) + j) * 6
                o[idx + 0] = v0.toShort() //Bottom triangle
                o[idx + 1] = v2.toShort()
                o[idx + 2] = v3.toShort()
                o[idx + 3] = v0.toShort() //Top triangle
                o[idx + 4] = v3.toShort()
                o[idx + 5] = v1.toShort()
            }
        }
        return o;
    }

    private fun calculateTextureMapping(): FloatArray {
        val w = 1.0f / (gridColumns - 1)
        val h = 1.0f / (gridRows - 1)
        val textureMap = FloatArray((vertexPosition.size / 3) * 2) //map 1 texture to each vertex
        for (i in 0..gridRows - 1) {
            for (j in 0..gridColumns - 1) {
                val p = 2 * (i * gridColumns + j)
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
        return -0.2f * (y * y);
    }
}