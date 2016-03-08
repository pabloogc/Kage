
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
const float RAD_SCALE_X = 1.0;
const float RAD_SCALE_Y = 2.0;
const float RAD = 0.25 * RAD_SCALE_X;
const float RAD_PROJECTED = RAD * PI; //Half circuference

//float test_right_side(vec2);
//vec2 rotate_vec2(float, vec2, vec2);

//rotation in radians, vector to rotate, origin
vec2 rotate_vec2(float rad, vec2 origin, vec2 point){
//    p'x = cos(theta) * (px-ox) - sin(theta) * (py-oy) + ox
//    p'y = sin(theta) * (px-ox) + cos(theta) * (py-oy) + oy
    vec2 vp = vec2(0.0f, 0.0f);
    float c = cos(rad);
    float s = sin(rad);
    vp.x = c * (point.x-origin.x) - s * (point.y - origin.y) + origin.x;
    vp.y = s * (point.x-origin.x) + c * (point.y - origin.y) + origin.y;
    return vp;
}

mat4 rotation_matrix(vec3 axis, float angle)
{
    axis = normalize(axis);
    float s = sin(angle);
    float c = cos(angle);
    float oc = 1.0 - c;

    return mat4(oc * axis.x * axis.x + c,           oc * axis.x * axis.y - axis.z * s,  oc * axis.z * axis.x + axis.y * s,  0.0,
                oc * axis.x * axis.y + axis.z * s,  oc * axis.y * axis.y + c,           oc * axis.y * axis.z - axis.x * s,  0.0,
                oc * axis.z * axis.x - axis.y * s,  oc * axis.y * axis.z + axis.x * s,  oc * axis.z * axis.z + c,           0.0,
                0.0,                                0.0,                                0.0,                                1.0);
}

float angle(vec2 v) {
    return atan(v.y / v.x);
}

float test_right_side(vec2 rect_point, vec2 dir, vec2 point) {
    //http://math.stackexchange.com/questions/274712/calculate-on-which-side-of-straign-line-is-dot-located
    //To determine which side of the line from A=(x1,y1) to B=(x2,y2)
    //a point P=(x,y)P=(x,y) falls on you need to compute the value:
    //(x−x1)(y2−y1)−(y−y1)(x2−x1)
    vec2 a = rect_point;
    vec2 b = vec2(dir.x + touch.x, touch.y + dir.y);
    return (point.x - a.x)*(b.y - a.y) - (point.y - a.y)*(b.x - a.x);
}

float distance_vec_to_point(vec2 rect_point, vec2 dir, vec2 point){
    vec2 p1 = rect_point;
    vec2 p2 = vec2(dir.x + touch.x, touch.y + dir.y);

    float x1 = p1.x;
    float y1 = p1.y;

    float x2 = p2.x;
    float y2 = p2.y;

    float x0 = point.x;
    float y0 = point.y;

    float num = (y2-y1)*x0 - (x2-x1)*y0 + x2*y1 - y2*x1;
    float denom = (y2-y1) * (y2-y1) + (x2-x1) * (x2-x1);
    return abs(num) / sqrt(denom);
}

vec2 perpendicular_point_to_line(vec2 rect_point, vec2 dir, vec2 point){
    vec2 perp = vec2(-dir.y, dir.x);
    return (distance_vec_to_point(rect_point, dir, point) * perp);
}

void main() {
    fragColor = color;
    fragTextCoord = textCoord;

    vec4 v = vec4(position.x, position.y, position.z, 1.0);
    vec2 cilinderCenter = vec2(touch.x, RAD);


    float test_right = test_right_side(touch, direction, position.xy);
    if(test_right <= 0.0) {
        //position.x - touch.x < PI * RAD
        vec2 perp_vector = perpendicular_point_to_line(touch, direction, position.xy);
        float distanceToRect = length(perp_vector);
        float distanceToBackFace = (distanceToRect) / RAD_PROJECTED;

        float rotationZ = min(PI * distanceToBackFace, PI);
        float rotationX = -(PI_HALF + abs(angle(direction)));

        vec2 cilinderCenter = vec2(position.x - perp_vector.x, RAD);
        v.xz = rotate_vec2(rotationZ, cilinderCenter, vec2(cilinderCenter.x, 0));
        v.xy = rotate_vec2(rotationX, position.xy - perp_vector, position.xy);
        v.z *= RAD_SCALE_Y;
        v.x += 0.05;

//        float distanceToBackFace = (position.x - touch.x) / RAD_PROJECTED;
//        if(distanceToBackFace <= 1.0){ //Crest
//            float rotation = PI * distanceToBackFace;
//            v.xz = rotate_vec2(rotation, cilinderCenter, vec2(touch.x, 0));

        if(distanceToBackFace <= 1.0){ //Crest
            float color = max(0.3, mix(0.0, 2.0 * RAD_SCALE_Y * RAD, v.z));
            fragColor = vec4(color, color, color, 1.0);
        }

//            v.xz = rotate_vec2(rotationZ, cilinderCenter, vec2(touch.x, 0));
        }

//    if(!TRANSFORM){
//            v = position;
//            float d = touch.x - position.x;
//            if(test_right_side(touch, vec2(0.0, -1.0), position.xy) < 0.0){
//            mat4 r = rotation_matrix(vec3(0.0, direction.y, 0.0), PI * d * 0.5);
//            v.x = v.x - d;
//            v = r * v;
//            v.x = v.x + d;
//            }
//        }
//

    gl_PointSize = 1.0f;
    gl_Position = mvp * v;
}

