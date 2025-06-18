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
void _umainImage(out vec4 _uO, in vec2 _uI);
out vec4 _ushadertoy_out_color;
void _umainImage(out vec4 _uO, in vec2 _uI){
  (_uO = vec4(0.0, 0.0, 0.0, 0.0));
  float _ut = _uiTime;
  float _ui = 0.0;
  float _uz = 0.0;
  float _ud = 0.0;
  float _us = 0.0;
  for ((_uO *= _ui); ((_ui++) < 100.0); )
  {
    vec3 _up = (_uz * normalize((vec3((_uI + _uI), 0) - _uiResolution.xyy)));
    for ((_ud = 5.0); (_ud < 200.0); (_ud += _ud))
    {
      (_up += ((0.600000024 * sin(((_up.yzx * _ud) - (0.200000003 * _ut)))) / _ud));
    }
    (_uz += (_ud = (0.00499999989 + (max((_us = (0.300000012 - abs(_up.y))), ((-_us) * 0.200000003)) / 4.0))));
    (_uO += (((cos(((((_us / 0.0700000003) + _up.x) + (0.5 * _ut)) - vec4(3.0, 4.0, 5.0, 0.0))) + 1.5) * exp((_us / 0.100000001))) / _ud));
  }
  (_uO = tanh(((_uO * _uO) / 400000000.0)));
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