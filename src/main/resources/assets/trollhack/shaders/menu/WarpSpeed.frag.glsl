#version 450
uniform vec3 _uiResolution;
uniform float _uiTime;
uniform float _uiChannelTime[4];
uniform vec4 _uiMouse;
uniform vec4 _uiDate;
uniform float _uiSampleRate;
uniform vec3 _uiChannelResolution[4];
uniform int _uiFrame;
uniform float _uiTimeDelta;
uniform float _uiFrameRate;
uniform sampler2D _uiChannel0;
uniform struct {
    vec3 _usize;
    float _utime;
    int _uloaded;
    sampler2D _usampler;
} _uiCh0;
uniform sampler2D _uiChannel1;
uniform struct {
    vec3 _usize;
    float _utime;
    int _uloaded;
    sampler2D _usampler;
} _uiCh1;
uniform sampler2D _uiChannel2;
uniform struct {
    vec3 _usize;
    float _utime;
    int _uloaded;
    sampler2D _usampler;
} _uiCh2;
uniform sampler2D _uiChannel3;
uniform struct {
    vec3 _usize;
    float _utime;
    int _uloaded;
    sampler2D _usampler;
} _uiCh3;
void _umainImage(out vec4 _ufragColor, in vec2 _ufragCoord);
out vec4 _ushadertoy_out_color;
void _umainImage(out vec4 _ufragColor, in vec2 _ufragCoord){
    (_ufragColor = vec4(0.0, 0.0, 0.0, 0.0));
    float _us = 0.0;
    float _uv = 0.0;
    vec2 _uuv = (((_ufragCoord / _uiResolution.xy) * 2.0) - 1.0);
    float _utime = ((_uiTime - 2.0) * 58.0);
    vec3 _ucol = vec3(0.0, 0.0, 0.0);
    vec3 _uinit = vec3((sin((_utime * 0.00319999992)) * 0.300000012), (0.349999994 - (cos((_utime * 0.00499999989)) * 0.300000012)), (_utime * 0.00200000009));
    for (int _ur = 0; (_ur < 100); (_ur++))
    {
        vec3 _up = (_uinit + (_us * vec3(_uuv, 0.0500000007)));
        (_up.z = fract(_up.z));
        for (int _ui = 0; (_ui < 10); (_ui++))
        {
            (_up = ((abs((_up * 2.03999996)) / dot(_up, _up)) - 0.899999976));
        }
        (_uv += (pow(dot(_up, _up), 0.699999988) * 0.0599999987));
        (_ucol += ((vec3(((_uv * 0.200000003) + 0.400000006), (12.0 - (_us * 2.0)), (0.100000001 + (_uv * 1.0))) * _uv) * 2.99999992e-05));
        (_us += 0.0250000004);
    }
    (_ufragColor = vec4(clamp(_ucol, 0.0, 1.0), 1.0));
}
void main(){
    (_ushadertoy_out_color = vec4(0.0, 0.0, 0.0, 0.0));
    (_ushadertoy_out_color = vec4(1.0, 1.0, 1.0, 1.0));
    vec4 _ucolor = vec4(100000002004087734272.0, 100000002004087734272.0, 100000002004087734272.0, 100000002004087734272.0);
    _umainImage(_ucolor, gl_FragCoord.xy);
    if ((_ushadertoy_out_color.x < 0.0))
    {
        (_ucolor = vec4(1.0, 0.0, 0.0, 1.0));
    }
    if ((_ushadertoy_out_color.y < 0.0))
    {
        (_ucolor = vec4(0.0, 1.0, 0.0, 1.0));
    }
    if ((_ushadertoy_out_color.z < 0.0))
    {
        (_ucolor = vec4(0.0, 0.0, 1.0, 1.0));
    }
    if ((_ushadertoy_out_color.w < 0.0))
    {
        (_ucolor = vec4(1.0, 1.0, 0.0, 1.0));
    }
    (_ushadertoy_out_color = vec4(_ucolor.xyz, 1.0));
}