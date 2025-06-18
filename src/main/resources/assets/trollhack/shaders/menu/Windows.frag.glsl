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
float _uhash(in vec2 _up){
    float _uh = dot(_up, vec2(127.099998, 311.700012));
    return ((fract((sin(_uh) * 458.325409)) * 2.0) - 1.0);
}
float _unoise(in vec2 _up){
    vec2 _ui = floor(_up);
    vec2 _uf = fract(_up);
    (_uf = ((_uf * _uf) * (3.0 - (2.0 * _uf))));
    return mix(mix(_uhash((_ui + vec2(0.0, 0.0))), _uhash((_ui + vec2(1.0, 0.0))), _uf.x), mix(_uhash((_ui + vec2(0.0, 1.0))), _uhash((_ui + vec2(1.0, 1.0))), _uf.x), _uf.y);
}
vec2 _urot(in vec2 _up, in float _ua){
    return vec2(((_up.x * cos(_ua)) - (_up.y * sin(_ua))), ((_up.x * sin(_ua)) + (_up.y * cos(_ua))));
}
float _unac(in vec3 _up, in vec2 _uF, in vec3 _uo){
    (_up += _uo);
    return (length(max((abs(_up.xy) - vec2(_uF)), 0.0)) - 9.99999975e-05);
}
float _urecta(in vec3 _up, in vec3 _uF, in vec3 _uo){
    (_up += _uo);
    return (length(max((abs(_up) - _uF), 0.0)) - 9.99999975e-05);
}
float _umap1(in vec3 _up, in float _uscale){
    float _uG = 0.5;
    float _uF = (0.5 * _uscale);
    float _ut = _unac(_up, vec2(_uF, _uF), vec3(_uG, _uG, 0.0));
    (_ut = min(_ut, _unac(_up, vec2(_uF, _uF), vec3(_uG, (-_uG), 0.0))));
    (_ut = min(_ut, _unac(_up, vec2(_uF, _uF), vec3((-_uG), _uG, 0.0))));
    (_ut = min(_ut, _unac(_up, vec2(_uF, _uF), vec3((-_uG), (-_uG), 0.0))));
    return _ut;
}
float _umap2(in vec3 _up){
    float _ut = _umap1(_up, 0.899999976);
    (_ut = max(_ut, _urecta(_up, vec3(1.0, 1.0, 0.0199999996), vec3(0.0, 0.0, 0.0))));
    return _ut;
}
float _ugennoise(in vec2 _up){
    float _ud = 0.5;
    mat2 _uh = mat2(1.60000002, 1.20000005, -1.20000005, 1.60000002);
    float _ucolor = 0.0;
    for (int _ui = 0; (_ui < 2); (_ui++))
    {
        (_ucolor += (_ud * _unoise(((_up * 5.0) + _uiTime))));
        (_up *= _uh);
        (_ud /= 2.0);
    }
    return _ucolor;
}
void _umainImage(out vec4 _ufragColor, in vec2 _ufragCoord){
    (_ufragColor = vec4(0.0, 0.0, 0.0, 0.0));
    (_ufragColor = vec4(0.0, 0.0, 0.0, 0.0));
    for (int _ucount = 0; (_ucount < 2); (_ucount++))
    {
        vec2 _uuv = (-1.0 + (2.0 * (_ufragCoord.xy / _uiResolution.xy)));
        (_uuv *= 1.39999998);
        (_uuv.x += (_uhash(((_uuv.xy + _uiTime) + float(_ucount))) / 512.0));
        (_uuv.y += (_uhash(((_uuv.yx + _uiTime) + float(_ucount))) / 512.0));
        vec3 _udir = normalize(vec3((_uuv * vec2((_uiResolution.x / _uiResolution.y), 1.0)), (1.0 + (sin(_uiTime) * 0.00999999978))));
        (_udir.xz = _urot(_udir.xz, 1.22173059));
        (_udir.xy = _urot(_udir.xy, 1.57079637));
        vec3 _upos = vec3((-0.100000001 + (sin((_uiTime * 0.300000012)) * 0.100000001)), (2.0 + (cos((_uiTime * 0.400000006)) * 0.100000001)), -3.5);
        vec3 _ucol = vec3(0.0, 0.0, 0.0);
        float _ut = 0.0;
        float _uM = 1.00199997;
        float _ubsh = 0.00999999978;
        float _udens = 0.0;
        for (int _ui = min(_uiFrame, 0); (_ui < 600); (_ui++))
        {
            float _utemp = _umap1((_upos + (_udir * _ut)), 0.600000024);
            if ((_utemp < 0.200000003))
            {
                (_ucol += (vec3(0.00249999994, 0.00349999988, 0.00850000046) * _udens));
            }
            (_ut += (_ubsh * _uM));
            (_ubsh *= _uM);
            (_udens += 0.0250000004);
        }
        (_ut = 0.0);
        float _uy = 0.0;
        for (int _ui = min(_uiFrame, 0); (_ui < 25); (_ui++))
        {
            float _utemp = _umap2((_upos + (_udir * _ut)));
            if ((_utemp < 0.0250000004))
            {
                (_ucol += vec3(0.075000003, 0.400000006, 0.850000024));
            }
            (_ut += _utemp);
            (_uy++);
        }
        (_ucol += (((2.0 + _uuv.x) * vec3(0.150000006, 0.800000012, 1.70000005)) + (_uy / 1250.0)));
        (_ucol += (_ugennoise(_udir.xz) * 0.5));
        (_ucol *= (1.0 - (_uuv.y * 0.5)));
        (_ucol *= vec3(0.0500000007, 0.0500000007, 0.0500000007));
        (_ucol = pow(_ucol, vec3(0.717000008, 0.717000008, 0.717000008)));
        (_ufragColor += vec4(_ucol, (1.0 / _ut)));
    }
    (_ufragColor /= vec4(2.0, 2.0, 2.0, 2.0));
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