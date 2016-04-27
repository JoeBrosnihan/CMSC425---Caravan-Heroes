
precision mediump float;

uniform sampler2D uTexture;
uniform vec2 uvOrigin;
uniform vec2 uvScale;

uniform vec4 uColorMultiplier;
uniform bool uTextureless;

#define maxLights 16
uniform vec3 lightPos[maxLights];
uniform vec3 lightColor[maxLights];
uniform int nLights;

varying vec3 pos;
varying vec3 norm;
varying vec2 uv;

void main() {
	vec3 totalLight = vec3(.5f, .5f, .5f);
	vec3 posToLight;
	float ndot;
	for (int i = 0; i < nLights; i++) {
		posToLight = lightPos[i] - pos;
		ndot = dot(normalize(posToLight), norm);
		if (ndot > 0.0f) {
            float dist = length(posToLight);
            //float intensity = ndot / ((dist + 0.707106781186547f) * (dist + 0.707106781186547f));
            float intensity = ndot * (dist + 1.0f) / (2.0f * dist * dist * dist + dist + 1.0f);
            totalLight = totalLight + intensity * lightColor[i];
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
