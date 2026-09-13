# encoding: UTF-8
require 'sketchup.rb'
require 'json'
ZS_SKIP_AUTOBUILD=true
load 'E:/众墅之家AI赋能平台底座/outputs/sketchup_villa_20260909/build_villa.rb'
UI.start_timer(1,false) do
  out='E:/众墅之家AI赋能平台底座/outputs/sketchup_villa_20260909'
  begin
    m=Sketchup.active_model
    raise 'Wrong model' unless m.get_attribute('模型说明','来源')
    m.start_operation('细化编织花格并整理最终模型',true)
    root=m.entities.grep(Sketchup::Group).find{|g|g.name.start_with?('众墅之家')}
    ent=root.entities.grep(Sketchup::Group).find{|g|g.name.start_with?('05')}.entities
    helpers=ZSReferenceVilla
    helpers.instance_variable_set(:@model,m)
    cubes={};m.definitions.each{|df|cubes[df.name.sub('基本块_','')]=df if df.name.start_with?('基本块_')}
    helpers.instance_variable_set(:@cubes,cubes)
    metal=m.materials['深古铜窗框'];recess=m.materials['雕刻阴槽']
    # Replace only the generated transom and middle-door ornament, preserving frames.
    old=ent.grep(Sketchup::Group).select do |g|
      b=g.bounds
      g.name=='雕刻条' && b.min.x>=(-1.2).m && b.max.x<=(1.2).m && b.min.z>=(3.30).m && b.max.z<=(6.29).m
    end
    ent.erase_entities(old) unless old.empty?
    screen=helpers.group(ent,'中央门楣_三股编织花格（可编辑组件）')
    18.times do |ix|
      14.times do |iz|
        x=-1.08+ix*0.12;z=4.54+iz*0.12
        3.times do |k|
          off=0.015+k*0.032
          if (ix+iz).even?
            helpers.box(screen.entities,x+0.006,0.043,z+off,0.111,0.055,0.012,metal,'横向编织条')
          else
            helpers.box(screen.entities,x+off,0.040,z+0.006,0.012,0.057,0.111,metal,'竖向编织条')
          end
        end
      end
    end
    [-0.72,0.72].each{|x|helpers.box(screen.entities,x-0.012,0.024,4.53,0.024,0.08,1.69,metal,'花格分格竖梃')}
    carving=helpers.group(ent,'大门中段_斜向回折纹')
    # Diagonal basket-weave, with strokes clipped to the middle panel rectangle.
    conv=lambda{|u,v|[(u-v)*0.70710678,3.88+(u+v)*0.70710678]}
    17.times do |i|
      17.times do |j|
        u=(i-8)*0.16;v=(j-8)*0.16
        2.times do |k|
          aa,bb=(i+j).even? ? [conv.call(u,v+k*0.048),conv.call(u+0.14,v+k*0.048)] : [conv.call(u+k*0.048,v),conv.call(u+k*0.048,v+0.14)]
          next unless [aa,bb].all?{|x,z|x.abs<1.05 && z>3.36 && z<4.40}
          helpers.bar(carving.entities,[aa[0],0.097,aa[1]],[bb[0],0.097,bb[1]],0.013,0.026,recess)
        end
      end
    end
    m.materials['暖白石材'].color=[244,240,230]
    m.materials['米灰主墙石材'].color=[230,226,216]
    m.materials['浅灰檐底'].color=[197,193,186]
    # Avoid incidental show-through across the inferred rear windows in presentation.
    m.materials['灰蓝玻璃'].alpha=0.90
    m.set_attribute('模型说明','花格细化','三股交错编织几何；斜向回折中段；按效果图特征解释重建')
    m.commit_operation
    ro=m.rendering_options
    {'EdgeDisplayMode'=>0,'DrawSilhouettes'=>false,'DrawProfilesOnly'=>false,'DrawBackEdges'=>false,'DrawGround'=>false,'DrawHorizon'=>false,'DisplaySketchAxes'=>false,'ModelTransparency'=>false,'MaterialTransparency'=>true,'Texture'=>true,'BackgroundColor'=>Sketchup::Color.new(225,232,239)}.each{|k,v|ro[k]=v}
    si=m.shadow_info;si['DisplayShadows']=true;si['Light']=95;si['Dark']=62;si['UseSunForAllShading']=true
    si['ShadowTime']=Time.utc(2026,9,9,13,30,0)
    m.styles.update_selected_style
    m.pages.each{|pg|pg.use_rendering_options=false;pg.use_shadow_info=false}
    view=m.active_view;pt=lambda{|x,y,z|[x.m,y.m,z.m]}
    m.layers['09 庭院景观'].visible=false
    view.camera=Sketchup::Camera.new(pt.call(0,-35,5.60),pt.call(0,0,5.60),Z_AXIS,false);view.camera.height=13.2.m
    m.pages[0].update;view.refresh
    view.write_image(filename:File.join(out,'01_front.png'),width:2100,height:1700,antialias:true)
    m.layers['09 庭院景观'].visible=true
    view.camera=Sketchup::Camera.new(pt.call(23,-32,19),pt.call(0,0.8,3.9),Z_AXIS,true);view.camera.fov=43
    m.pages[1].update;view.refresh
    view.write_image(filename:File.join(out,'02_perspective.png'),width:2100,height:1550,antialias:true)
    m.layers['09 庭院景观'].visible=false
    view.camera=Sketchup::Camera.new(pt.call(0,-25,4.25),pt.call(0,0,4.25),Z_AXIS,false);view.camera.height=8.35.m
    m.pages[2].update;view.refresh
    view.write_image(filename:File.join(out,'03_entry_detail.png'),width:1500,height:1600,antialias:true)
    m.layers['09 庭院景观'].visible=true
    view.camera=Sketchup::Camera.new(pt.call(0,-48,3.25),pt.call(0,0,5.65),Z_AXIS,true);view.camera.fov=24.5
    m.pages[3].update;view.refresh
    view.write_image(filename:File.join(out,'04_front_perspective.png'),width:2100,height:1700,antialias:true)
    m.styles.update_selected_style
    destination=File.join(out,'众墅之家_14米外观模型_最终.skp')
    raise 'Final save failed' unless m.save(destination)
    # Count reusable geometry definitions and their valid faces after the final save.
    face_count=m.definitions.inject(0){|sum,df|sum+df.entities.grep(Sketchup::Face).length}
    report={status:'saved_and_checked',file:m.path,sketchup_version:Sketchup.version,main_width_m:14,assumed_depth_m:10,central_fascia_m:11.22,top_groups:root.entities.grep(Sketchup::Group).map(&:name),scenes:m.pages.map(&:name),definitions:m.definitions.count,definition_faces:face_count,materials:m.materials.count,sun_direction:si['SunDirection'].to_a,model_xray:ro['ModelTransparency'],limits:['尺寸为用户同意的约14米面宽假定；未进行测量','侧后、室内、屋面后部推定','微雕为近似几何，绿化为示意']}
    File.write(File.join(out,'model_report.json'),JSON.pretty_generate(report))
    File.write(File.join(out,'final_check.json'),JSON.pretty_generate(report))
  rescue Exception=>e
    File.write(File.join(out,'finish_error.txt'),e.full_message)
  end
end
