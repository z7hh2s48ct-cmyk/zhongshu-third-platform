# encoding: UTF-8
# SketchUp 2022 native geometry. All coordinates are metres, stored at full scale.
require 'sketchup.rb'
require 'json'

module ZSReferenceVilla
  extend self
  OUT = 'E:/众墅之家AI赋能平台底座/outputs/sketchup_villa_20260909'
  def p(x,y,z); [x.m,y.m,z.m]; end
  def log(s); File.open(File.join(OUT,'build.log'),'a'){|f|f.puts("#{Time.now}: #{s}")}; end
  def material(name,rgb,alpha=1)
    m=@model.materials.add(name); m.color=rgb; m.alpha=alpha; m
  end
  def group(parent,name,tag=nil)
    g=parent.add_group; g.name=name; g.layer=@model.layers.add(tag) if tag; g
  end
  def face(e,pts,mat)
    f=e.add_face(pts.map{|a|p(*a)}); return unless f
    f.material=mat; f.back_material=mat; f
  end
  def box(e,x,y,z,w,d,h,mat,name=nil)
    return if [w,d,h].any?{|n| n<=0.0001}
    key=mat.name
    unless @cubes[key]
      df=@model.definitions.add("基本块_#{key}")
      f=df.entities.add_face(p(0,0,0),p(1,0,0),p(1,1,0),p(0,1,0));f.reverse! if f.normal.z<0;f.pushpull(1.m)
      df.entities.grep(Sketchup::Face).each{|s|s.material=mat;s.back_material=mat}
      @cubes[key]=df
    end
    t=Geom::Transformation.translation(p(x,y,z))*Geom::Transformation.scaling(w,d,h)
    i=e.add_instance(@cubes[key],t);i.name=name if name;i
  end
  def poly_y(e,points,y,depth,mat,name='浮雕')
    g=group(e,name); f=face(g.entities,points.map{|x,z|[x,y,z]},mat)
    if f
      f.reverse! if f.normal.y>0;f.pushpull(-depth.m)
      g.entities.grep(Sketchup::Face).each{|s|s.material=mat;s.back_material=mat}
    end
    g
  end
  def bar(e,a,b,w,d,mat)
    dx=b[0]-a[0];dz=b[2]-a[2];len=Math.sqrt(dx*dx+dz*dz);return if len<0.0001
    ux=-dz/len*w/2;uz=dx/len*w/2
    poly_y(e,[[a[0]+ux,a[2]+uz],[b[0]+ux,b[2]+uz],[b[0]-ux,b[2]-uz],[a[0]-ux,a[2]-uz]],a[1],d,mat,'雕刻条')
  end
  def frame(e,x,y,z,w,h,t,dep,mat)
    box(e,x,y,z,t,dep,h,mat);box(e,x+w-t,y,z,t,dep,h,mat)
    box(e,x+t,y,z,w-2*t,dep,t,mat);box(e,x+t,y,z+h-t,w-2*t,dep,t,mat)
  end
  def roundel(e,x,y,z,r,mat)
    pts=(0...24).map{|i|a=i*Math::PI/12;[x+r*Math.cos(a),z+r*Math.sin(a)]}
    poly_y(e,pts,y,0.035,mat,'圆形纹章')
  end
  def rosette(e,x,y,z,r)
    roundel(e,x,y+0.015,z,r,@recess)
    8.times do |i|
      a=i*Math::PI/4
      pts=[[0.08,0],[0.38,0.15],[0.89,0.08],[0.57,-0.15]].map{|u,v|[x+r*(u*Math.cos(a)-v*Math.sin(a)),z+r*(u*Math.sin(a)+v*Math.cos(a))]}
      poly_y(e,pts,y-0.018,0.027,@white,'莲瓣')
    end
    roundel(e,x,y-0.04,z,r*0.16,@white)
  end
  def greek(e,x,y,z,length,size,mat=@recess)
    n=(length/size).floor;return if n<1
    step=length/n
    n.times do |i|
      xx=x+i*step; s=step*0.84;h=size*0.72
      pts=[[0,0],[s,0],[s,h],[s*0.2,h],[s*0.2,h*0.28],[s*0.7,h*0.28],[s*0.7,h*0.7],[s*0.46,h*0.7]]
      pts.each_cons(2){|a,b|bar(e,[xx+a[0],y,z+a[1]],[xx+b[0],y,z+b[1]],0.012,0.009,mat)}
    end
  end
  def bevel_block(e,x,y,z,w,d,h,inset,mat)
    g=group(e,'收分线脚');q=g.entities
    a=[[x,y,z],[x+w,y,z],[x+w,y+d,z],[x,y+d,z]]
    b=[[x+inset,y+inset,z+h],[x+w-inset,y+inset,z+h],[x+w-inset,y+d-inset,z+h],[x+inset,y+d-inset,z+h]]
    face(q,a.reverse,mat);face(q,b,mat)
    4.times{|i|j=(i+1)%4;face(q,[a[i],a[j],b[j],b[i]],mat)};g
  end
  def cornice(e,x,y,z,w,d,central=false)
    g=group(e,central ? '中央挑檐' : '宽挑檐');q=g.entities
    box(q,x+0.38,y+0.38,z,w-0.76,d-0.76,0.13,@dark)
    bevel_block(q,x+0.39,y+0.39,z+0.13,w-0.78,d-0.78,0.50,-0.39,@soffit)
    # The outer dark rim stays horizontal; light soffit recedes inward below.
    box(q,x,y,z+0.60,w,d,0.18,@dark)
    box(q,x+0.02,y+0.015,z+0.75,w-0.04,d-0.03,0.035,@edge)
    n=(w/0.8).floor
    n.times do |i|
      xx=x+0.44+(w-0.88)*(i+0.5)/n
      box(q,xx-0.065,y+0.18,z+0.15,0.13,0.52,0.18,@white)
      box(q,xx-0.085,y+0.44,z,0.17,0.20,0.15,@stone)
      box(q,xx-0.09,y+0.41,z-0.01,0.18,0.27,0.055,@white)
    end
    g
  end
  def pillar(e,cx,y,bottom,height,w,small=false)
    g=group(e,small ? '上层雕花方柱' : '通高雕花方柱');q=g.entities
    box(q,cx-w*0.53,y-0.035,bottom,w*1.06,0.82,height*0.09,@dark)
    box(q,cx-w*0.58,y-0.08,bottom+height*0.09,w*1.16,0.91,0.09,@dark)
    shaft=height*0.70
    box(q,cx-w*0.43,y,bottom+height*0.09+0.09,w*0.86,0.74,shaft,@stone)
    box(q,cx-w*0.31,y-0.022,bottom+height*0.17,w*0.62,0.04,height*0.56,@white)
    count=small ? 3 : 5
    count.times do |i|
      xx=cx+(i-(count-1)/2.0)*w*0.048
      box(q,xx-0.008,y-0.032,bottom+height*0.22,0.016,0.012,height*0.40,@recess)
    end
    [height*0.15,height*0.79].each do |zz|
      box(q,cx-w*0.50,y-0.06,bottom+zz,w,0.86,0.10,@white)
      box(q,cx-w*0.47,y-0.07,bottom+zz+0.10,w*0.94,0.86,0.23,@stone)
      frame(q,cx-w*0.41,y-0.092,bottom+zz+0.125,w*0.82,0.15,0.018,0.022,@white)
      greek(q,cx-w*0.37,y-0.11,bottom+zz+0.15,w*0.74,0.14)
      box(q,cx-w*0.53,y-0.10,bottom+zz+0.32,w*1.06,0.94,0.06,@white)
    end
    z=bottom+height*0.86
    box(q,cx-w*0.43,y-0.01,z,w*0.86,0.76,height*0.1,@white)
    rosette(q,cx,y-0.035,z+height*0.048,w*0.13)
    box(q,cx-w*0.50,y-0.08,bottom+height-0.13,w,0.90,0.10,@dark)
    box(q,cx-w*0.57,y-0.13,bottom+height-0.035,w*1.14,1.0,0.075,@dark)
    unless small
      rosette(q,cx,y-0.043,bottom+height*0.715,w*0.13)
      9.times{|i|box(q,cx-w*0.34+i*w*0.085,y-0.09,bottom+height*0.85,w*0.037,0.04,0.12,@white)}
    end
    g
  end
  def window(e,x,y,z,w,h,style=:normal)
    g=group(e,'黑框蓝玻璃窗');q=g.entities
    box(q,x-0.15,y-0.025,z-0.13,w+0.30,0.22,0.12,@white)
    frame(q,x-0.13,y,z-0.04,w+0.26,h+0.17,0.11,0.22,@stone)
    frame(q,x-0.05,y-0.055,z-0.005,w+0.1,h+0.02,0.025,0.075,@recess)
    frame(q,x,y-0.08,z,w,h,0.055,0.13,@metal)
    box(q,x+0.05,y+0.035,z+0.05,w-0.1,0.021,h-0.1,@glass)
    cols=style==:tall ? [0.25,0.73] : [0.23,0.75]
    cols.each{|t|box(q,x+w*t-0.022,y-0.075,z,0.045,0.10,h,@metal)}
    rows=case style;when :tall then [0.35,0.57,0.78];when :upper then [0.65];else [0.82];end
    rows.each{|t|box(q,x,y-0.075,z+h*t-0.022,w,0.10,0.045,@metal)}
    if style==:tall
      box(q,x,y-0.075,z+0.74*h,w*0.25,0.10,0.038,@metal)
      box(q,x+0.74*w,y-0.075,z+0.13*h,w*0.26,0.10,0.045,@metal)
    else
      box(q,x,y-0.075,z+h*0.24,w*0.23,0.10,0.04,@metal)
      box(q,x+w*0.75,y-0.075,z+h*0.27,w*0.25,0.10,0.04,@metal)
    end
    g
  end
  def opening_wall(e,x,w,z,h,ox,ow,oz,oh,y=0)
    box(e,x,y,z,ox-x,0.32,h,@stone)
    box(e,ox+ow,y,z,x+w-ox-ow,0.32,h,@stone)
    box(e,ox,y,z,ow,0.32,oz-z,@stone)
    box(e,ox,y,oz+oh,ow,0.32,z+h-oz-oh,@stone)
  end
  def balustrade(e,x,y,z,w)
    g=group(e,'石雕露台栏杆');q=g.entities
    box(q,x,y,z,w,0.23,0.075,@white)
    box(q,x-0.045,y-0.035,z+0.49,w+0.09,0.30,0.075,@stone)
    box(q,x-0.06,y-0.045,z+0.565,w+0.12,0.32,0.025,@white)
    n=(w/0.30).floor
    n.times do |i|
      cx=x+w*(i+0.5)/n
      pts=[[-0.053,0.08],[-0.080,0.115],[-0.065,0.18],[-0.044,0.24],[-0.072,0.36],[-0.061,0.45],[-0.077,0.49],[0.077,0.49],[0.061,0.45],[0.072,0.36],[0.044,0.24],[0.065,0.18],[0.080,0.115],[0.053,0.08]].map{|a,b|[cx+a,z+b]}
      poly_y(q,pts,y+0.025,0.18,@white,'石雕栏板')
      frame(q,cx-0.031,y+0.012,z+0.24,0.062,0.15,0.012,0.016,@recess)
      roundel(q,cx,y+0.008,z+0.40,0.016,@recess)
    end
    g
  end
  def rail_post(e,x,y,z,w=0.55,h=0.92)
    q=group(e,'栏杆端柱').entities
    box(q,x-w/2,y,z,w,0.58,h,@stone)
    box(q,x-w/2-0.035,y-0.04,z+h,w+0.07,0.66,0.07,@white)
    box(q,x-w/2-0.075,y-0.06,z+h+0.07,w+0.15,0.71,0.045,@stone)
    frame(q,x-0.145,y-0.022,z+0.19,0.29,0.30,0.022,0.04,@recess)
    bar(q,[x-0.12,y-0.04,z+0.22],[x+0.12,y-0.04,z+0.46],0.018,0.02,@recess)
    bar(q,[x+0.12,y-0.04,z+0.22],[x-0.12,y-0.04,z+0.46],0.018,0.02,@recess)
  end
  def hip_roof(e,x,y,z,w,d,rise)
    q=group(e,'推定四坡灰瓦屋面').entities
    inset=[w*0.24,d*0.26].min
    a=[[x,y,z],[x+w,y,z],[x+w,y+d,z],[x,y+d,z]]
    b=[[x+inset,y+inset,z+rise],[x+w-inset,y+inset,z+rise],[x+w-inset,y+d-inset,z+rise],[x+inset,y+d-inset,z+rise]]
    4.times{|i|j=(i+1)%4;face(q,[a[i],a[j],b[j],b[i]],@tile)};face(q,b,@tile)
    rows=8
    rows.times do |i|
      t=(i+0.25)/rows;ix=inset*t;zz=z+rise*t+0.012
      box(q,x+ix,y+ix,zz,w-ix*2,0.025,0.018,@tileline)
      box(q,x+ix,y+d-ix-0.025,zz,w-ix*2,0.025,0.018,@tileline)
      box(q,x+ix,y+ix,zz,0.025,d-ix*2,0.018,@tileline)
      box(q,x+w-ix-0.025,y+ix,zz,0.025,d-ix*2,0.018,@tileline)
    end
  end
  def sphere_def(mat)
    key=mat.name;return @spheres[key] if @spheres[key]
    df=@model.definitions.add("植物团簇_#{key}");q=df.entities
    n=12;m=8
    m.times do |j|
      n.times do |i|
        a=i*2*Math::PI/n;b=(i+1)*2*Math::PI/n;c=-Math::PI/2+j*Math::PI/m;d=-Math::PI/2+(j+1)*Math::PI/m
        pts=[[a,c],[b,c],[b,d],[a,d]].map{|u,v|[Math.cos(v)*Math.cos(u),Math.cos(v)*Math.sin(u),Math.sin(v)]}
        pts=pts.uniq{|v|v.map{|k|k.round(6)}};face(q,pts,mat) if pts.length>=3
      end
    end
    q.grep(Sketchup::Edge).each{|ed|ed.soft=true;ed.smooth=true};@spheres[key]=df
  end
  def shrub(e,x,y,z,rx,ry,rz,mat)
    df=sphere_def(mat);e.add_instance(df,Geom::Transformation.translation(p(x,y,z))*Geom::Transformation.scaling(rx,ry,rz))
  end
  def build
    log('Starting model')
    @model=Sketchup.active_model;@cubes={};@spheres={}
    raise 'Expected task working template only' unless ['working','working_template','villa_working'].include?(@model.title)
    @model.start_operation('效果图外观建模',true)
    # Preserve template entities in a hidden group rather than changing source assets.
    unless @model.entities.length.zero?
      old=@model.entities.add_group(@model.entities.to_a);old.name='原始模板（隐藏）';old.hidden=true
    end
    @model.options['UnitsOptions']['LengthUnit']=2
    @model.options['UnitsOptions']['LengthFormat']=0
    @model.options['UnitsOptions']['LengthPrecision']=0
    @white=material('暖白石材',[229,224,214]);@stone=material('米灰主墙石材',[205,200,191])
    @recess=material('雕刻阴槽',[151,146,142]);@dark=material('深灰檐口与基座',[54,51,50])
    @edge=material('檐口压边',[71,66,65]);@soffit=material('浅灰檐底',[174,170,163])
    @metal=material('深古铜窗框',[39,38,37]);@glass=material('灰蓝玻璃',[106,150,178],0.74)
    @door=material('乌木门板',[49,41,36]);@gold=material('香槟金铜饰',[172,139,77])
    @tile=material('深灰瓦',[73,80,83]);@tileline=material('瓦楞',[102,109,112])
    @lamp=material('壁灯暖白灯罩',[249,231,180]);@paving=material('庭院灰石',[157,159,155])
    @paving2=material('步道浅灰石',[183,182,172]);@soil=material('花池深土',[83,75,58])
    @grass=material('草坪',[104,127,72]);@green=material('灌木深绿',[51,82,40]);@leaf=material('灌木浅绿',[104,129,64])
    @trunk=material('树干',[96,84,63]);@flower=material('白色花簇',[238,237,216])
    root=group(@model.entities,'众墅之家｜参考图外观复原｜14m推定尺寸');@root=root
    root.set_attribute('建模依据','尺度','主体宽14m，进深10m，最高檐口11.22m；按单张效果图推定')
    root.set_attribute('建模依据','限制','未提供图纸；背面、侧面、室内、结构和微雕纹样不属于已验证复原。')
    arch=group(root.entities,'01 建筑主体','01 建筑主体');e=arch.entities
    box(e,-7,0,0,14,10,0.50,@dark,'主体基座 14000 × 10000mm')
    box(e,-7,0,0.5,0.32,10,6.60,@stone,'西侧墙（推定）');box(e,6.68,0,0.5,0.32,10,6.60,@stone,'东侧墙（推定）')
    box(e,-6.68,9.68,0.5,13.36,0.32,6.60,@stone,'背墙（推定）')
    box(e,-6.68,0.32,0.50,13.36,9.36,0.16,@stone,'首层楼板')
    box(e,-6.68,3.4,3.92,13.36,6.28,0.20,@stone,'后部二层楼板（推定）')
    box(e,2.9,0.32,3.92,3.78,3.08,0.20,@stone,'右侧二层楼板（推定）')
    box(e,-6.68,0.32,6.94,13.36,9.36,0.20,@stone,'露台楼板')
    opening_wall(e,-7,4.25,0.50,6.60,-5.62,2.45,0.95,5.60)
    opening_wall(e,2.75,4.25,0.50,6.60,3.17,2.45,0.95,5.60)
    box(e,3.17,0,3.35,2.45,0.32,1.18,@stone,'右侧窗间石饰板')
    opening_wall(e,-2.75,5.50,0.50,6.60,-1.23,2.46,0.55,5.78,0.45)
    # Ashlar joints and recessed strips, geometric rather than a photo decal.
    facade=group(root.entities,'02 正立面石材分缝','02 石材及线脚');f=facade.entities
    [-6.9,-6.17,-3.05,-2.8,2.8,3.0,6.17,6.90].each{|x|box(f,x-0.011,-0.013,0.75,0.022,0.016,6.12,@recess)}
    [1.45,2.45,3.45,4.45,5.45,6.45].each{|z|[[-7,1.30],[-3.13,0.37],[2.76,0.4],[5.64,1.36]].each{|x,w|box(f,x,-0.014,z,w,0.015,0.015,@recess)}}
    log('Main walls and floors ready')
    col=group(root.entities,'03 四根通高石柱及柱头','03 石柱浮雕');c=col.entities
    pillar(c,-6.58,-0.48,0.02,7.02,0.84);pillar(c,6.58,-0.48,0.02,7.02,0.84)
    pillar(c,-2.34,-0.96,0.02,7.42,1.0);pillar(c,2.34,-0.96,0.02,7.42,1.0)
    win=group(root.entities,'04 非对称窗墙','04 门窗');w=win.entities
    window(w,-5.62,-0.14,0.95,2.45,5.60,:tall)
    window(w,3.17,-0.14,0.95,2.45,2.40,:normal)
    window(w,3.17,-0.14,4.53,2.45,2.02,:normal)
    [-5.76,3.03].each{|x|greek(f,x,-0.24,0.79,2.72,0.16);greek(f,x,-0.16,6.74,2.72,0.16)}
    # Chamfered octagonal spandrel with nested stone profiles and lotus centre.
    q=group(f,'右窗间八角莲花饰板').entities
    [[0,0,@white], [0.09,-0.025,@recess], [0.14,-0.05,@white], [0.23,-0.072,@recess]].each do |ins,yy,mat|
      x1=3.11+ins;x2=5.68-ins;z1=3.49+ins*0.65;z2=4.39-ins*0.65;cut=0.22
      poly_y(q,[[x1+cut,z1],[x2-cut,z1],[x2,z1+cut],[x2,z2-cut],[x2-cut,z2],[x1+cut,z2],[x1,z2-cut],[x1,z1+cut]],-0.20+yy,0.04,mat)
    end
    rosette(q,4.395,-0.31,3.94,0.23)
    greek(q,3.53,-0.265,4.29,1.74,0.14);greek(q,3.53,-0.265,3.53,1.74,0.14)
    entry=group(root.entities,'05 门廊及铜饰花格大门','05 门廊大门');d=entry.entities
    # Continuous dark recessed portal with a full height transom screen.
    box(d,-1.22,0.39,0.56,2.44,0.11,5.72,@door)
    frame(d,-1.24,-0.01,0.55,2.48,5.75,0.075,0.38,@metal)
    box(d,-1.08,0.17,0.58,2.16,0.09,2.63,@door)
    frame(d,-1.07,0.075,0.58,2.14,2.64,0.049,0.08,@gold)
    box(d,-0.023,0.07,0.60,0.046,0.09,2.60,@gold)
    [-0.83,0.78].each{|x|frame(d,x,0.066,0.66,0.055,2.42,0.011,0.03,@gold)}
    [-0.08,0.08].each{|x|box(d,x-0.025,-0.035,1.68,0.05,0.14,0.35,@gold)}
    poly_y(d,[[-0.15,1.72],[-0.19,1.78],[-0.19,1.97],[-0.12,2.03],[0.12,2.03],[0.19,1.97],[0.19,1.78],[0.15,1.72]],-0.07,0.06,@gold,'双开门锁饰')
    frame(d,-1.16,0.11,3.27,2.32,1.20,0.055,0.06,@metal)
    9.times do |j|
      x=-1.05+j*0.25
      bar(d,[x,0.10,3.35],[x+0.74,0.10,4.39],0.019,0.025,@recess) if x+0.74<1.16
      bar(d,[x,0.105,4.39],[x+0.74,0.105,3.35],0.019,0.025,@recess) if x+0.74<1.16
    end
    box(d,-1.10,0.27,4.52,2.20,0.021,1.73,@glass)
    frame(d,-1.13,0.08,4.48,2.26,1.81,0.06,0.10,@metal)
    # Interlocking Chinese fret screen: repeating offset right-angle paths.
    10.times do |ix|
      8.times do |iz|
        xx=-1.04+ix*0.207;zz=4.57+iz*0.202
        pts=if (ix+iz).even?;[[0,0],[0,0.16],[0.145,0.16],[0.145,0.05],[0.052,0.05],[0.052,0.108]];else;[[0.16,0],[0.02,0],[0.02,0.145],[0.115,0.145],[0.115,0.053]];end
        pts.each_cons(2){|a,b|bar(d,[xx+a[0],0.055,zz+a[1]],[xx+b[0],0.055,zz+b[1]],0.021,0.055,@metal)}
      end
    end
    # Decorative portal surround and stepped hanging brackets.
    box(d,-1.76,-0.47,6.30,3.52,0.94,0.25,@white)
    box(d,-1.91,-0.50,6.55,3.82,0.98,0.13,@white)
    greek(d,-1.70,-0.514,6.38,3.4,0.16)
    [-1,1].each do |s|
      pts=[[1.23,5.61],[1.23,6.32],[1.71,6.32],[1.64,6.08],[1.51,6.08],[1.44,5.88],[1.35,5.88]].map{|x,z|[x*s,z]}
      poly_y(d,pts,-0.35,0.62,@white,'阶梯式门楣雀替')
    end
    [-1.34,-0.67,0,0.67,1.34].each{|x|frame(d,x-0.30,-0.47,6.70,0.60,0.23,0.025,0.035,@recess)}
    rosette(d,0,-0.52,6.815,0.13)
    [-2.34,2.34].each do |x|
      box(d,x-0.115,-1.02,3.16,0.23,0.10,0.94,@metal)
      box(d,x-0.069,-1.11,3.20,0.138,0.15,0.84,@lamp)
      box(d,x-0.09,-1.14,3.19,0.035,0.20,0.88,@gold)
      box(d,x+0.055,-1.14,3.19,0.035,0.20,0.88,@gold)
    end
    log('Doors, windows, carved columns ready')
    roof=group(root.entities,'06 挑檐及露台','06 屋檐露台');r=roof.entities
    cornice(r,-7.70,-1.16,6.97,15.4,11.85)
    cornice(r,-3.38,-1.73,7.39,6.76,3.15,true)
    # Upper storey appears above the terrace, with a higher central pavilion.
    up=group(root.entities,'07 上层退台与高起中厅','07 上层建筑');u=up.entities
    box(u,-6.35,0.66,7.73,12.7,8.85,0.15,@stone)
    box(u,-6.35,0.66,7.85,0.28,8.85,1.67,@stone);box(u,6.07,0.66,7.85,0.28,8.85,1.67,@stone)
    box(u,-6.07,9.23,7.85,12.14,0.28,1.67,@stone)
    opening_wall(u,-6.35,3.80,7.85,1.67,-5.15,2.21,7.96,1.19,0.66)
    opening_wall(u,2.55,3.80,7.85,1.67,2.94,2.21,7.96,1.19,0.66)
    opening_wall(u,-2.55,5.10,7.85,2.68,-1.02,2.04,8.20,1.40,0.13)
    box(u,-2.55,0.45,9.45,0.28,8.8,1.08,@stone);box(u,2.27,0.45,9.45,0.28,8.8,1.08,@stone)
    box(u,-2.55,9.23,9.45,5.1,0.28,1.08,@stone)
    window(u,-5.15,0.50,7.96,2.21,1.19,:upper);window(u,2.94,0.50,7.96,2.21,1.19,:upper)
    window(u,-1.02,-0.03,8.20,2.04,1.4,:upper)
    [-6.02,6.02].each{|x|pillar(u,x,0.22,7.84,1.88,0.76,true)}
    [-2.08,2.08].each{|x|pillar(u,x,-0.24,8.15,2.45,0.72,true)}
    [-1.17,1.17].each do |x|
      box(u,x-0.13,-0.065,9.57,0.26,0.30,0.82,@white)
      box(u,x-0.19,-0.13,10.33,0.38,0.39,0.11,@white)
    end
    box(u,-1.08,-0.065,9.73,2.16,0.29,0.30,@stone)
    greek(u,-1.01,-0.082,9.75,2.02,0.16)
    [-0.80,0,0.80].each{|x|rosette(u,x,-0.09,9.88,0.092)}
    15.times{|i|box(u,-0.52+i*0.074,-0.09,9.83,0.025,0.027,0.18,@white)}
    balustrade(r,-6.35,-0.69,7.77,3.81);balustrade(r,2.54,-0.69,7.77,3.81)
    [-6.65,6.65].each{|x|rail_post(r,x,-0.72,7.75,0.49,0.80)}
    balustrade(r,-2.05,-1.19,8.20,4.10)
    [-2.37,2.37].each{|x|rail_post(r,x,-1.22,8.17,0.52,0.88)}
    cornice(r,-7.10,-0.15,9.41,14.20,10.29)
    hip_roof(r,-6.85,0.10,10.195,13.70,9.80,0.60)
    cornice(r,-3.16,-0.84,10.435,6.32,10.70,true)
    hip_roof(r,-2.97,-0.66,11.15,5.94,10.33,0.06)
    # Roof ridge is inferred; the highest visible fascia is 11.22 m.
    log('Upper terraces and roofs ready')
    inferred=group(root.entities,'08 侧后立面门窗（推定）','08 侧后立面推定');ie=inferred.entities
    # Additional windows use independent rotated assemblies, clearly identified.
    [-1,1].each do |s|
      [2.2,5.4,8.0].each do |yy|
        [1.0,4.55].each do |zz|
          g=group(ie,'侧窗（无图纸推定）');window(g.entities,-1.0,0,zz,2.0,1.75)
          g.transform!(Geom::Transformation.translation(p(s*7.02,yy,0))*Geom::Transformation.rotation(ORIGIN,Z_AXIS,s*Math::PI/2))
        end
      end
    end
    [-4.65,-1.45,1.65,4.8].each do |xx|
      [1.0,4.55].each do |zz|
        g=group(ie,'背窗（无图纸推定）');window(g.entities,xx-0.95,0,zz,1.9,1.7)
        g.transform!(Geom::Transformation.translation(p(0,10.03,0))*Geom::Transformation.rotation(ORIGIN,Z_AXIS,Math::PI))
      end
    end
    site=group(root.entities,'09 前庭台阶、铺地与绿化示意','09 庭院景观');s=site.entities
    box(s,-13,-14,-0.20,26,27,0.18,@grass,'场地')
    box(s,-8.4,-2.45,-0.03,16.8,13.4,0.07,@paving,'建筑周边石材平台')
    box(s,-2.06,-2.29,0,4.12,1.50,0.16,@white,'第一级台阶')
    box(s,-2.06,-1.95,0.16,4.12,1.16,0.17,@white,'第二级台阶')
    box(s,-2.06,-1.61,0.33,4.12,0.82,0.17,@white,'第三级台阶')
    [-2.57,2.57].each{|x|box(s,x-0.44,-1.35,0,0.88,1.05,0.52,@dark,'门廊石墩')}
    8.times do |j|
      9.times do |i|
        xx=-6.28+i*1.40;yy=-2.55-j*0.68
        box(s,xx,yy,-0.018,1.37,0.65,0.07,(i+j)%4==0 ? @paving2 : @paving)
      end
    end
    9.times do |j|
      4.times do |i|
        box(s,-2.15+i*1.08,-8.7-j*0.58,-0.035,1.055,0.55,0.075,(i+j)%3==0 ? @paving : @paving2)
      end
    end
    srand(142022)
    [-1,1].each do |side|
      box(s,side<0 ? -9.8 : 3.1,-12.0,-0.05,6.7,9.0,0.05,@soil)
      20.times do |i|
        yy=-3.4-i*0.42;xx=side*(3.15+0.06*Math.sin(i))
        shrub(s,xx,yy,0.33,0.43,0.39,0.37,@green)
      end
      45.times do |i|
        xx=side*(3.8+rand*5.7);yy=-3.4-rand*8.1
        rr=0.23+rand*0.46
        shrub(s,xx,yy,rr*0.75,rr,rr*0.85,rr*0.85,i%3==0 ? @leaf : @green)
        if i%4==0
          7.times{|k|a=k*Math::PI/3.5;shrub(s,xx+rr*0.70*Math.cos(a),yy+rr*0.7*Math.sin(a),rr*1.35,0.055,0.055,0.045,@flower)}
        end
      end
      # Modest tree masses at plot edges keep the facade unobstructed.
      [[10.5,-1.5,6.5],[11.3,-9.5,7.2]].each do |xx,yy,hh|
        box(s,side*xx-0.11,yy-0.11,0,0.22,0.22,hh*0.70,@trunk)
        8.times{|k|a=k*Math::PI/4;shrub(s,side*xx+1.1*Math.cos(a),yy+0.85*Math.sin(a),hh-0.3+0.5*Math.sin(k*2),1.25,1.20,1.35,k.even? ? @green : @leaf)}
      end
      box(s,side<0 ? -13 : 7.8,3.0,0,5.2,0.23,1.75,@white,'庭院围墙')
      box(s,side<0 ? -13.05 : 7.75,2.90,1.75,5.3,0.43,0.12,@tile)
    end
    log('Landscape ready')
    dims=group(root.entities,'10 尺度标注（推定，默认隐藏）','10 尺度标注');de=dims.entities
    de.add_dimension_linear(p(-7,0,0),p(7,0,0),p(0,-3,0))
    de.add_dimension_linear(p(7,0,0),p(7,10,0),p(3,0,0))
    de.add_dimension_linear(p(-7,0,0),p(-7,0,11.22),p(-3,0,0))
    dims.hidden=true
    @model.set_attribute('模型说明','来源','用户提供单张正面效果图。约14米面宽由用户确认作为初始假定。')
    @model.set_attribute('模型说明','尺度','1:1 实体米制建模；不表示经测量确认的原建筑尺寸。')
    @model.set_attribute('模型说明','进深和高度','主体14×10m；中央最高水平檐口约11.22m；背侧及屋面背部推定。')
    @model.set_attribute('模型说明','用途','外观设计参考；非施工图、结构模型或精确测绘成果。')
    ro=@model.rendering_options
    {'DisplayColorByLayer'=>false,'DrawGround'=>false,'DrawHorizon'=>false,'BackgroundColor'=>Sketchup::Color.new(235,239,241),'EdgeDisplayMode'=>0,'DrawSilhouettes'=>false,'DrawProfilesOnly'=>false,'DisplaySketchAxes'=>false,'DisplayFog'=>false,'ModelTransparency'=>false,'MaterialTransparency'=>true,'Texture'=>true}.each{|k,v|begin;ro[k]=v;rescue;end}
    @model.shadow_info['DisplayShadows']=true
    @model.shadow_info['Light']=85;@model.shadow_info['Dark']=62
    @model.shadow_info['ShadowTime']=Time.utc(2026,9,9,2,30,0)
    @model.shadow_info['Latitude']=30.0;@model.shadow_info['Longitude']=120.0
    @model.shadow_info['TZOffset']=8.0
    @model.commit_operation
    camera=Sketchup::Camera.new(p(0,-35,6.1),p(0,0,5.5),Z_AXIS,false);camera.height=14.7.m
    @model.active_view.camera=camera
    @model.pages.add('01 正立面｜效果图比例')
    @model.active_view.refresh
    @model.active_view.write_image(filename:File.join(OUT,'01_front.png'),width:2100,height:1700,antialias:true,transparent:false)
    @model.active_view.camera=Sketchup::Camera.new(p(22,-29,19),p(0,2.0,4.7),Z_AXIS,true)
    @model.active_view.camera.fov=42
    @model.pages.add('02 东南鸟瞰｜侧后推定')
    @model.active_view.refresh
    @model.active_view.write_image(filename:File.join(OUT,'02_perspective.png'),width:2100,height:1550,antialias:true,transparent:false)
    @model.layers['09 庭院景观'].visible=false
    @model.active_view.camera=Sketchup::Camera.new(p(0,-25,4.4),p(0,0,4.4),Z_AXIS,false);@model.active_view.camera.height=8.3.m
    @model.pages.add('03 门廊细节')
    @model.active_view.refresh
    @model.active_view.write_image(filename:File.join(OUT,'03_entry_detail.png'),width:1500,height:1600,antialias:true,transparent:false)
    @model.layers['09 庭院景观'].visible=true
    @model.active_view.camera=camera
    @model.pages.selected_page=@model.pages[0]
    path=File.join(OUT,'众墅之家_效果图复原_14米假定尺寸.skp')
    raise 'Save failed' unless @model.save(path)
    File.write(File.join(OUT,'model_report.json'),JSON.pretty_generate({sketchup_version:Sketchup.version,path:path,main_width_m:14,assumed_depth_m:10,central_fascia_m:11.22,component_definitions:@model.definitions.count,materials:@model.materials.count,tags:@model.layers.map(&:name),scenes:@model.pages.map(&:name),root_bounds_m:[root.bounds.width.to_m,root.bounds.height.to_m,root.bounds.depth.to_m],status:'saved',limitations:['Single reference image, no measured drawings','Side/rear and interior inferred','Carvings interpreted; planting simplified']}))
    log('DONE saved '+path)
  rescue Exception => e
    @model.abort_operation rescue nil
    log("ERROR #{e.class}: #{e.message}\n#{e.backtrace.join("\n")}")
  end
end
UI.start_timer(1.0,false){ZSReferenceVilla.build} unless defined?(ZS_SKIP_AUTOBUILD)
