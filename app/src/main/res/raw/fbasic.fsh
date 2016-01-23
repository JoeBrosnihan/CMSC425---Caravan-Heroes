
precision mediump float;

uniform sampler2D uTexture;
uniform vec2 uvOrigin;
uniform vec2 uvScale;

uniform vec4 uColorMultiplier;
uniform bool uTextureless;

#define maxLights 1
uniform vec3 lightPos[maxLights];
uniform vec3 lightColor[maxLights];

varying vec3 pos;
varying vec3 norm;
varying vec2 uv;

void main() {
	vec3 totalLight = vec3(.5f, .5f, .5f);
	vec3 posToLight;
	float ndot;
	for (int i = 0; i < maxLights; i++) {
		//posToLight = lightPos[i] - pos;
		posToLight = vec3(5, -1, 3) - pos;
		ndot = dot(normalize(posToLight), norm);
		if (ndot > 0.0f) {
			//totalLight = totalLight + ndot * lightColor[i];
			totalLight = totalLight + ndot * vec3(1, .9f, .8f);
		} else {
			totalLight = totalLight - ndot * vec3(1, .9f, .8f);
		}
	}
    vec4 sample;
    if (uTextureless)
        sample = vec4(1.f, 1.f, 1.f, 1.f);
	else
        sample = texture2D(uTexture, uvOrigin + uv * uvScale);

    sample *= uColorMultiplier;
	if (sample.w < .1)
		discard;
	gl_FragColor = vec4(totalLight * sample.xyz, sample.w);
}
