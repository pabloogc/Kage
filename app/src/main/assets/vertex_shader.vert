
uniform mat4 mvp;
uniform vec2 touch;
uniform vec2 direction;

attribute vec4 color;
attribute vec4 position;
attribute vec2 textCoord;

varying vec4 fragColor;
varying vec2 fragTextCoord;

#define DEBUG true
#define TRANSFORM false
#define ORIGIN vec2(0.0, 0.0)

#define PI (3.14159265358979)
#define PI_2 (PI * 2.0)
#define PI_HALF (PI / 2.0)

const vec4 BACK_FACE_COLOR = vec4(0.4, 0.4, 0.4, 0.4);
const float RAD = 0.25;
const float RAD_PROJECTED = RAD * PI; //Half circuference

//float test_right_side(vec2);
//vec2 rotate_vec2(float, vec2, vec2);

//rotation in radians, vector to rotate, origin
vec2 rotate_vec2(float rad, vec2 vector, vec2 origin){
//    p'x = cos(theta) * (px-ox) - sin(theta) * (py-oy) + ox
//    p'y = sin(theta) * (px-ox) + cos(theta) * (py-oy) + oy
    vec2 vp = vec2(0.0f, 0.0f);
    float c = cos(rad);
    float s = sin(rad);
    vp.x = c * (vector.x-origin.x) - s * (vector.y - origin.y) + origin.x;
    vp.y = s * (vector.x-origin.x) + c * (vector.y - origin.y) + origin.y;
    return vp;
}

float test_right_side(vec2 dir, vec2 point) {
    //http://math.stackexchange.com/questions/274712/calculate-on-which-side-of-straign-line-is-dot-located
    //To determine which side of the line from A=(x1,y1) to B=(x2,y2)
    //a point P=(x,y)P=(x,y) falls on you need to compute the value:
    //(x−x1)(y2−y1)−(y−y1)(x2−x1)
    vec2 a = touch;
    vec2 b = vec2(dir.x + touch.x, touch.y + dir.y);
//    vec2 b = vec2(1.0,1.0);
    return (point.x - a.x)*(b.y - a.y) - (point.y - a.y)*(b.x - a.x);
}

void main() {
    fragColor = color;
    fragTextCoord = textCoord;

//    vec2 touch = touch;
//    touch.x = touch.x + touch.x * RAD_PROJECTED;

    vec4 v = vec4(position.x, position.y, position.z, 1.0);
    vec2 cilinderCenter = vec2(touch.x, RAD);

    //float htx = (1.0 + touch.x) / 2.0;

    if(position.x > touch.x) {
        //position.x - touch.x < PI * RAD
        float distanceToBackFace = (position.x - touch.x) / RAD_PROJECTED;
        if(distanceToBackFace <= 1.0){ //Crest
            float rotation = PI * distanceToBackFace;
            v.xz = rotate_vec2(rotation, vec2(touch.x, 0), cilinderCenter);
            if(distanceToBackFace > 0.5f) fragColor = BACK_FACE_COLOR;
            if(DEBUG) fragColor = vec4(0.0, 1.0 - distanceToBackFace, 0.0, 1.0);
        } else { //Back face
            v.xz = rotate_vec2(PI, position.xz, vec2(touch.x + RAD_PROJECTED, 0));
            v.x = v.x - RAD_PROJECTED;
            v.z = 2.0 * RAD;
            fragColor = BACK_FACE_COLOR;
            if(DEBUG) fragColor = vec4(1.0, 0.0, 0.0, 0.0);
         }
    }

    if(!TRANSFORM) v = position;

    fragColor = vec4(1.0, 1.0, 1.0, 1.0);

    if(test_right_side(rotate_vec2(0.0, direction.xy, ORIGIN), position.xy) > 0.0){
//        if(test_right_side(direction.xy, position.xy) < 0.0){
            fragColor.r = 0.0;
//        }
    }

    if(test_right_side(vec2(-direction.y, direction.x), position.xy) < 0.0){
                fragColor.b = 0.0;
    }




    gl_PointSize = 1.0f;
    gl_Position = mvp * v;
}

