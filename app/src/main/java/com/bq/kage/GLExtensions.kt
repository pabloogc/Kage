package com.bq.kage

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLES20.GL_VERTEX_SHADER
import android.opengl.GLES20.GL_FRAGMENT_SHADER
import android.support.annotation.IntDef
import java.io.ByteArrayOutputStream

@IntDef(GL_VERTEX_SHADER.toLong(), GL_FRAGMENT_SHADER.toLong())
@Retention(AnnotationRetention.SOURCE)
annotation class ShaderType;

public data class Shader(val shader: Int) {
  companion object {
    fun fromAsset(@ShaderType type: Int, context: Context, file: String): Shader {
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
      val shader = GLES20.glCreateShader(type)
      GLES20.glShaderSource(shader, source)
      GLES20.glCompileShader(shader)
      return Shader(shader)
    }
  }
}

public data class Program(
    val program: Int,
    val vertexShader: Shader,
    val fragmentShader: Shader
) {
  fun use() {
    GLES20.glUseProgram(program)
  }

  fun getAtribute(string: String) {

  }
}

fun program(init: ProgramBuilder.() -> Unit): Program {
  val builder = ProgramBuilder()
  builder.init()
  val program = GLES20.glCreateProgram()
  GLES20.glAttachShader(program, builder.vertexShader.shader)
  GLES20.glAttachShader(program, builder.fragmentShader.shader)
  GLES20.glLinkProgram(program)
  return Program(program, builder.vertexShader, builder.fragmentShader)
}

public class ProgramBuilder {
  lateinit var vertexShader: Shader   private set
  lateinit var fragmentShader: Shader   private set

  fun vertexShader(shader: Shader) {
    vertexShader = shader
  }

  fun fragmentShader(shader: Shader) {
    fragmentShader = shader
  }
}
