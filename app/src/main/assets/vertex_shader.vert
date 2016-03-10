
uniform float bounds[4];
uniform mat4 mvp;
uniform vec2 apex;
uniform vec2 direction;

attribute vec4 color;
attribute vec4 position;
attribute vec2 textCoord;

varying vec4 fragColor;
varying vec2 fragTextCoord;

#define DEBUG false
#define TRANSFORM true

#define PI (3.14159265358979)
#define PI_2 (PI * 2.0)
#define PI_HALF (PI / 2.0)

const vec4 BACK_FACE_COLOR = vec4(0.4, 0.4, 0.4, 0.4);

const float RAD_X = 0.5;
const float RAD_Y = 0.25;
const float RAD = sqrt(RAD_X * RAD_X + RAD_Y * RAD_Y);
const float CURL_DISTANCE = PI * RAD_Y;

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

float rect_eq(vec2 rect_point, vec2 rect_direction, float x){
    float slope = rect_direction.y / rect_direction.x;
    return slope * (x - rect_point.x) + rect_point.y;
}

float rect_eq_inv(vec2 rect_point, vec2 rect_direction, float y){
    float slope_inv = rect_direction.x / rect_direction.y;
    return slope_inv * (y - rect_point.y) + rect_point.x;
}

float test_line_side(vec2 rect_point, vec2 dir, vec2 point) {
    //http://math.stackexchange.com/questions/274712/calculate-on-which-side-of-straign-line-is-dot-located
    //To determine which side of the line from A=(x1,y1) to B=(x2,y2)
    //a point P=(x,y)P=(x,y) falls on you need to compute the value:
    //(x−x1)(y2−y1)−(y−y1)(x2−x1)
    vec2 a = rect_point;
    vec2 b = vec2(dir.x + rect_point.x, rect_point.y + dir.y);
    return (point.x - a.x)*(b.y - a.y) - (point.y - a.y)*(b.x - a.x);
}

float distance_line_to_point(vec2 rect_point, vec2 dir, vec2 point){
    vec2 p1 = rect_point;
    vec2 p2 = vec2(dir.x + rect_point.x, rect_point.y + dir.y);

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

    float w = bounds[2] - bounds[0];
    float h = bounds[1] - bounds[3];

    vec4 v = vec4(position.x, position.y, position.z, 1.0);

    //Pass along
    fragColor = color;
    fragTextCoord = textCoord;

    float cross_distance = rect_eq_inv(apex, direction, v.y);
    float horizontal_distance_to_cone = abs(v.x - cross_distance);

    if(v.x > cross_distance) {

        vec2 perp_vector = perpendicular_line_to_point(apex, direction, v.xy);
        float distanceToRect = length(perp_vector);
        float proportionFromCurlStart = horizontal_distance_to_cone / (PI * 1.5) * ((h / w) *  cross_distance);
        float direction_angle = atan(perp_vector.y / perp_vector.x);


        //Curl up
        vec2 rotationPoint = vec2(v.x - horizontal_distance_to_cone, 0);

        // o = (
        //
        //      o      ·
        //      |      ·
        //      |      ·
        //      |    ··
        //      |  ··
        //______···_____ ... distanceToBackFaceProportion = 1 (CURL_DISTANCE)

        v.xz = rotate_vec2(proportionFromCurlStart * PI + PI_HALF, rotationPoint, vec2(v.x, 0));
        v.xy = rotate_vec2(direction_angle, vec2(rect_eq_inv(apex, direction, v.y), v.y), v.xy);

        if(DEBUG) fragColor = vec4(proportionFromCurlStart,  1.0 - proportionFromCurlStart, 0.0, 1.0);

        //Rotate every point along the division rect
    }

    if(!TRANSFORM) v = position;

    gl_PointSize = 10.0f;
    gl_Position = mvp * v;
}

