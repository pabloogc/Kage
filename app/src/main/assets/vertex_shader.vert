
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

float rect_eq_inv(vec2 point, vec2 direction, float y){
    float slope = direction.x / direction.y;
    return slope * (y - point.y) + point.x;
}

float test_line_side(vec2 rect_point, vec2 dir, vec2 point) {
    //http://math.stackexchange.com/questions/274712/calculate-on-which-side-of-straign-line-is-dot-located
    //To determine which side of the line from A=(x1,y1) to B=(x2,y2)
    //a point P=(x,y)P=(x,y) falls on you need to compute the value:
    //(x−x1)(y2−y1)−(y−y1)(x2−x1)
    vec2 a = rect_point;
    vec2 b = vec2(dir.x + touch.x, touch.y + dir.y);
    return (point.x - a.x)*(b.y - a.y) - (point.y - a.y)*(b.x - a.x);
}

float distance_line_to_point(vec2 rect_point, vec2 dir, vec2 point){
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

vec2 perpendicular_line_to_point(vec2 rect_point, vec2 dir, vec2 point){
    vec2 perp = vec2(-dir.y, dir.x);
    return (distance_line_to_point(rect_point, dir, point) * perp);
}

void main() {

    gl_PointSize = 10.0f;

    fragColor = color;
    fragTextCoord = textCoord;

    vec4 v = vec4(position.x, position.y, position.z, 1.0);
    vec2 cilinderCenter = vec2(touch.x, RAD);

    float test_right = test_line_side(touch, direction, position.xy);

    if(test_right <= 0.0) {
        //position.x - touch.x < PI * RAD
        vec2 perp_vector = perpendicular_line_to_point(touch, direction, position.xy);
        float distanceToRect = length(perp_vector);
        float distanceToBackFaceProportion = (distanceToRect) / RAD_PROJECTED;
        float direction_angle = atan(perp_vector.y / perp_vector.x);

        if(distanceToBackFaceProportion <= 1.0){
            //Curl up
            vec2 rotationPoint = vec2(v.x - perp_vector.x, RAD);

            //
            //      o      ·
            //      |      ·
            //      |      ·
            //      |    ··
            //      |  ··
            //______···_____ ... distanceToBackFace = 1 (RAD_PROJECTED)

            v.xz = rotate_vec2(distanceToBackFaceProportion * PI, rotationPoint, vec2(rotationPoint.x, 0));

            if(DEBUG) fragColor = vec4(1.0, 1.0, 0.0, 1.0);
        }

        //Rotate every point along the division rect
        v.xy = rotate_vec2(direction_angle, vec2(rect_eq_inv(touch, direction, v.y), v.y), v.xy);

        if(distanceToBackFaceProportion > 1.0) {
            //Map the rest of the points (outside the curl) to a flat surface

            vec2 n = normalize(perp_vector);
            gl_PointSize = 5.0f;
            v.xy = rotate_vec2(PI, v.xy - n* (distanceToRect - 0.5 * RAD_PROJECTED), v.xy);
            v.z = 2.0 * RAD;

            if(DEBUG) fragColor = vec4(1.0, 0.0, 1.0, 1.0);
        }
    }

    gl_Position = mvp * v;
}

