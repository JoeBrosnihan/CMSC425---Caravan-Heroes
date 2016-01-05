
// attribute, varying, in: what are the differences?

uniform mat4 modelMatrix;
uniform mat4 MVP;

attribute vec3 vPosition;
attribute vec2 vTexCoords;

varying vec3 pos;
varying vec3 norm;
varying vec2 uv;

void main() {
	vec4 homoPos = vec4(vPosition, 1.0f);
	gl_Position = MVP * homoPos;
	pos = (modelMatrix * homoPos).xyz;
	norm = mat3(modelMatrix) * vec3(0, 0, -1);
	uv = vTexCoords;
}
