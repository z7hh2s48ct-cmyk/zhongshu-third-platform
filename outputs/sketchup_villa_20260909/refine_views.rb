# encoding: UTF-8
require 'sketchup.rb'
require 'json'
UI.start_timer(1,false) do
  out='E:/众墅之家AI赋能平台底座/outputs/sketchup_villa_20260909'
  begin
    m=Sketchup.active_model
    raise 'Wrong model' unless m.get_attribute('模型说明','来源')
    ro=m.rendering_options
    {'EdgeDisplayMode'=>0,'DrawSilhouettes'=>false,'DrawProfilesOnly'=>false,'DrawBackEdges'=>false,'DisplaySketchAxes'=>false,'ModelTransparency'=>false,'MaterialTransparency'=>true,'Texture'=>true,'BackgroundColor'=>Sketchup::Color.new(225,232,239)}.each{|k,v|ro[k]=v}
    si=m.shadow_info
    si['DisplayShadows']=true;si['Light']=85;si['Dark']=62;si['Latitude']=30;si['Longitude']=120;si['TZOffset']=8
    si['ShadowTime']=Time.utc(2026,9,9,2,30,0)
    si['UseSunForAllShading']=true
    v=m.active_view
    pt=lambda{|x,y,z|[x.m,y.m,z.m]}
    m.pages.each{|pg|pg.use_camera=true}
    m.layers['09 庭院景观'].visible=false
    c=Sketchup::Camera.new(pt.call(0,-35,5.60),pt.call(0,0,5.60),Z_AXIS,false);c.height=13.2.m
    v.camera=c;m.pages[0].update
    v.refresh;v.write_image(filename:File.join(out,'01_front.png'),width:2100,height:1700,antialias:true)
    m.layers['09 庭院景观'].visible=true
    v.camera=Sketchup::Camera.new(pt.call(23,-32,19),pt.call(0,0.8,3.9),Z_AXIS,true);v.camera.fov=43
    m.pages[1].update
    v.refresh;v.write_image(filename:File.join(out,'02_perspective.png'),width:2100,height:1550,antialias:true)
    m.layers['09 庭院景观'].visible=false
    v.camera=Sketchup::Camera.new(pt.call(0,-25,4.25),pt.call(0,0,4.25),Z_AXIS,false);v.camera.height=8.35.m
    m.pages[2].update
    v.refresh;v.write_image(filename:File.join(out,'03_entry_detail.png'),width:1500,height:1600,antialias:true)
    m.layers['09 庭院景观'].visible=true
    # Long focal length and low eye height echo the supplied frontal rendering.
    v.camera=Sketchup::Camera.new(pt.call(0,-48,3.25),pt.call(0,0,5.65),Z_AXIS,true);v.camera.fov=24.5
    hero=m.pages.add('04 正面透视｜庭院效果');hero.update
    v.refresh;v.write_image(filename:File.join(out,'04_front_perspective.png'),width:2100,height:1700,antialias:true)
    m.save(File.join(out,'众墅之家_效果图复原_14米假定尺寸.skp'))
    File.write(File.join(out,'view_check.json'),JSON.pretty_generate({version:Sketchup.version,render:ro.to_h,shadow:si.to_h,scenes:m.pages.map(&:name),file:m.path,status:'refined_and_saved'}))
  rescue Exception=>e
    File.write(File.join(out,'refine_error.txt'),e.full_message)
  end
end
