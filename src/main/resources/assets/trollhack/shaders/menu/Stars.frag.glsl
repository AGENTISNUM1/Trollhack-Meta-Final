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
    vec2 _uuv = ((_ufragCoord.xy / _uiResolution.xy) - 0.5);
    (_uuv.y *= (_uiResolution.y / _uiResolution.x));
    vec3 _udir = vec3((_uuv * 0.800000012), 1.0);
    float _utime = ((_uiTime * 0.00999999978) + 0.25);
    float _ua1 = (0.5 + ((_uiMouse.x / _uiResolution.x) * 2.0));
    float _ua2 = (0.800000012 + ((_uiMouse.y / _uiResolution.y) * 2.0));
    mat2 _urot1 = mat2(cos(_ua1), sin(_ua1), (-sin(_ua1)), cos(_ua1));
    mat2 _urot2 = mat2(cos(_ua2), sin(_ua2), (-sin(_ua2)), cos(_ua2));
    (_udir.xz *= _urot1);
    (_udir.xy *= _urot2);
    vec3 _ufrom = vec3(1.0, 0.5, 0.5);
    (_ufrom += vec3((_utime * 2.0), _utime, -2.0));
    (_ufrom.xz *= _urot1);
    (_ufrom.xy *= _urot2);
    float _us = 0.100000001;
    float _ufade = 1.0;
    vec3 _uv = vec3(0.0, 0.0, 0.0);
    for (int _ur = 0; (_ur < 20); (_ur++))
    {
        vec3 _up = (_ufrom + ((_us * _udir) * 0.5));
        (_up = abs((vec3(0.850000024, 0.850000024, 0.850000024) - mod(_up, vec3(1.70000005, 1.70000005, 1.70000005)))));
        float _upa = 0.0;
        float _ua = (_upa = 0.0);
        for (int _ui = 0; (_ui < 17); (_ui++))
        {
            (_up = ((abs(_up) / dot(_up, _up)) - 0.529999971));
            (_ua += abs((length(_up) - _upa)));
            (_upa = length(_up));
        }
        float _udm = max(0.0, (0.300000012 - ((_ua * _ua) * 0.00100000005)));
        (_ua *= (_ua * _ua));
        if ((_ur > 6))
        {
            (_ufade *= (1.0 - _udm));
        }
        (_uv += _ufade);
        (_uv += (((vec3(_us, (_us * _us), (((_us * _us) * _us) * _us)) * _ua) * 0.00150000001) * _ufade));
        (_ufade *= 0.730000019);
        (_us += 0.100000001);
    }
    (_uv = mix(vec3(length(_uv)), _uv, 0.850000024));
    (_ufragColor = vec4((_uv * 0.00999999978), 1.0));
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