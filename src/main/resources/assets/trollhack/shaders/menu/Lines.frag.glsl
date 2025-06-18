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
float _uN21(in vec2 _up){
    vec3 _ua = fract((vec3(_up.xyx) * vec3(213.897003, 653.453003, 253.098007)));
    (_ua += dot(_ua, (_ua.yzx + 79.7600021)));
    return fract(((_ua.x + _ua.y) * _ua.z));
}
vec2 _uGetPos(in vec2 _uid, in vec2 _uoffs, in float _ut){
    float _un = _uN21((_uid + _uoffs));
    float _un1 = fract((_un * 10.0));
    float _un2 = fract((_un * 100.0));
    float _ua = (_ut + _un);
    return (_uoffs + (vec2(sin((_ua * _un1)), cos((_ua * _un2))) * 0.400000006));
}
float _udf_line(in vec2 _ua, in vec2 _ub, in vec2 _up){
    vec2 _upa = (_up - _ua);
    vec2 _uba = (_ub - _ua);
    float _uh = clamp((dot(_upa, _uba) / dot(_uba, _uba)), 0.0, 1.0);
    return length((_upa - (_uba * _uh)));
}
float _uline(in vec2 _ua, in vec2 _ub, in vec2 _uuv){
    float _ur1 = 0.0399999991;
    float _ur2 = 0.00999999978;
    float _ud = _udf_line(_ua, _ub, _uuv);
    float _ud2 = length((_ua - _ub));
    float _ufade = smoothstep(1.5, 0.5, _ud2);
    (_ufade += smoothstep(0.0500000007, 0.0199999996, abs((_ud2 - 0.75))));
    return (smoothstep(_ur1, _ur2, _ud) * _ufade);
}
float _uNetLayer(in vec2 _ust, in float _un, in float _ut){
    vec2 _uid = (floor(_ust) + _un);
    (_ust = (fract(_ust) - 0.5));
    vec2 _up[9] = vec2[9](vec2(0.0, 0.0), vec2(0.0, 0.0), vec2(0.0, 0.0), vec2(0.0, 0.0), vec2(0.0, 0.0), vec2(0.0, 0.0), vec2(0.0, 0.0), vec2(0.0, 0.0), vec2(0.0, 0.0));
    int _ui = 0;
    for (float _uy = -1.0; (_uy <= 1.0); (_uy++))
    {
        for (float _ux = -1.0; (_ux <= 1.0); (_ux++))
        {
            (_up[int(clamp(float((_ui++)), 0.0, 8.0))] = _uGetPos(_uid, vec2(_ux, _uy), _ut));
        }
    }
    float _um = 0.0;
    float _usparkle = 0.0;
    for (int _ui = 0; (_ui < 9); (_ui++))
    {
        (_um += _uline(_up[4], _up[int(clamp(float(_ui), 0.0, 8.0))], _ust));
        float _ud = length((_ust - _up[int(clamp(float(_ui), 0.0, 8.0))]));
        float _us = (0.00499999989 / (_ud * _ud));
        (_us *= smoothstep(1.0, 0.699999988, _ud));
        float _upulse = ((sin((((fract(_up[int(clamp(float(_ui), 0.0, 8.0))].x) + fract(_up[int(clamp(float(_ui), 0.0, 8.0))].y)) + _ut) * 5.0)) * 0.400000006) + 0.600000024);
        (_upulse = pow(_upulse, 20.0));
        (_us *= _upulse);
        (_usparkle += _us);
    }
    (_um += _uline(_up[1], _up[3], _ust));
    (_um += _uline(_up[1], _up[5], _ust));
    (_um += _uline(_up[7], _up[5], _ust));
    (_um += _uline(_up[7], _up[3], _ust));
    float _usPhase = (((sin((_ut + _un)) + sin((_ut * 0.100000001))) * 0.25) + 0.5);
    (_usPhase += (pow(((sin((_ut * 0.100000001)) * 0.5) + 0.5), 50.0) * 5.0));
    (_um += (_usparkle * _usPhase));
    return _um;
}
void _umainImage(out vec4 _ufragColor, in vec2 _ufragCoord){
    (_ufragColor = vec4(0.0, 0.0, 0.0, 0.0));
    vec2 _uuv = ((_ufragCoord - (_uiResolution.xy * 0.5)) / _uiResolution.y);
    vec2 _uM = ((_uiMouse.xy / _uiResolution.xy) - 0.5);
    float _ut = (_uiTime * 0.100000001);
    float _us = sin(_ut);
    float _uc = cos(_ut);
    mat2 _urot = mat2(_uc, (-_us), _us, _uc);
    vec2 _ust = (_uuv * _urot);
    (_uM *= (_urot * 2.0));
    float _um = 0.0;
    for (float _ui = 0.0; (_ui < 1.0); (_ui += 0.25))
    {
        float _uz = fract((_ut + _ui));
        float _usize = mix(15.0, 1.0, _uz);
        float _ufade = (smoothstep(0.0, 0.600000024, _uz) * smoothstep(1.0, 0.800000012, _uz));
        (_um += (_ufade * _uNetLayer(((_ust * _usize) - (_uM * _uz)), _ui, _uiTime)));
    }
    float _ufft = texelFetch(_uiChannel0, ivec2(0, 0), 0).x;
    float _uglow = (((-_uuv.y) * _ufft) * 2.0);
    vec3 _ubaseCol = ((vec3(_us, cos((_ut * 0.400000006)), (-sin((_ut * 0.239999995)))) * 0.400000006) + 0.600000024);
    vec3 _ucol = (_ubaseCol * _um);
    (_ucol += (_ubaseCol * _uglow));
    (_ucol *= (1.0 - dot(_uuv, _uuv)));
    (_ut = mod(_uiTime, 230.0));
    (_ucol *= (smoothstep(0.0, 20.0, _ut) * smoothstep(224.0, 200.0, _ut)));
    (_ufragColor = vec4(_ucol, 1));
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