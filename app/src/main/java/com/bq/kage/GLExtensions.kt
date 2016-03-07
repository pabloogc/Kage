package com.bq.kage

import android.content.Context
import android.opengl.GLES20.*
import android.opengl.GLU
import android.support.annotation.IntDef
import android.util.Log
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.util.*

@IntDef(GL_VERTEX_SHADER.toLong(), GL_FRAGMENT_SHADER.toLong())
@Retention(AnnotationRetention.SOURCE)
annotation class ShaderType;

public data class Shader(val shader: Int) {
    companion object {
        fun fromAsset(file: String, context: Context, @ShaderType type: Int): Shader {
            val stream = context.assets.open(file)
            val outStream = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var length = stream.read(buffer);

            while (length != -1) {
                outStream.write(buffer, 0, length);
                length = stream.read(buffer);
            }
            return fromString(type, outStream.toString("UTF-8"))
        }

        fun fromString(@ShaderType type: Int, source: String): Shader {
            val shader = glCreateShader(type).glCheck()
            glShaderSource(shader, source).glCheck()
            glCompileShader(shader).glCheck()
            val state = intArrayOf(0)
            glGetShaderiv(shader, GL_COMPILE_STATUS, state, 0)
            if (state[0] == 0) {
                val s = glGetShaderInfoLog(shader)
                Log.e("Shader", "Error compiling: $s")
                throw IllegalStateException(s)
            }
            return Shader(shader)
        }
    }
}

public data class Program(
        val program: Int,
        val shaders: List<Shader>
) {

    fun enable() {
        glUseProgram(program)
    }

    fun getAttribAndEnable(name: String): Int {
        val attr = glGetAttribLocation(program, name)
        glEnableVertexAttribArray(attr).glCheck()
        return attr
    }

    fun getAttribLocation(name: String) = glGetAttribLocation(program, name)

    fun getUniformLocation(name: String) = glGetUniformLocation(program, name)

    fun disable() {
        glCheckException()
    }
}

fun program(init: ProgramBuilder.() -> Unit): Program {
    val builder = ProgramBuilder()
    builder.init()
    val program = glCreateProgram().glCheck()

    for (shaderBuilder in builder.shaders) {
        glAttachShader(program, shaderBuilder.shader.shader).glCheck()
    }

    for (shaderBuilder in builder.shaders) {
        shaderBuilder.bindAttrs(program).glCheck()
    }

    glLinkProgram(program).glCheck()

    return Program(program, builder.shaders.map { it.shader })
}

public class ProgramBuilder {
    val shaders: ArrayList<ShaderBuilder> = ArrayList()

    fun shader(init: ShaderBuilder.() -> Unit) {
        val builder = ShaderBuilder()
        builder.init()
        shaders.add(builder)
    }
}

public class ShaderBuilder {
    lateinit var shader: Shader
    var type: Int = 0
    private val attrs = ArrayList<String>()
    private val attrsIds = ArrayList<Int>()

    public fun asset(context: Context, file: String, @ShaderType type: Int) {
        shader = Shader.fromAsset(file, context, type)
    }

    public fun load(f: () -> Shader) {
        this.shader = f()
    }

    public fun type(type: Int) {
        this.type = type
    }

    public fun attr(name: String, identifier: Int) {
        this.attrsIds.add(identifier)
        this.attrs.add(name)
    }

    fun bindAttrs(program: Int) {
        for (i in 0..attrs.size - 1) {
            glCheck {
                glBindAttribLocation(program, attrsIds[i], attrs[i]).glCheck()
            }
        }
    }

}

//Matrix utilities
fun FloatArray.length(): Float {
    val s = map { it * it }.sum().toDouble()
    return Math.sqrt(s).toFloat();
}

fun FloatArray.normalize(): FloatArray {
    val l = length()
    return map { it / l }.toFloatArray()
}


//Buffer utilities

fun FloatArray.sizeBytes(): Int = size * 4

fun FloatArray.toBuffer(): FloatBuffer {
    val out = ByteBuffer
            .allocateDirect(this.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
    out.put(this)
    out.position(0)
    return out
}

fun IntArray.toBuffer(): IntBuffer {
    val out = ByteBuffer
            .allocateDirect(this.size * 4)
            .order(ByteOrder.nativeOrder())
            .asIntBuffer()
    out.put(this)
    out.position(0)
    return out
}


//Error checking

public fun glCheckException() {
    val error = glGetError()
    if (error != GL_NO_ERROR) {
        throw IllegalStateException("OpenGL error: ${GLU.gluErrorString(error)}")
    }
}

fun <T> T.glCheck(): T {
    glCheckException()
    return this
}

inline fun glCheck(f: () -> Unit) {
    f()
    glCheckException()
}
