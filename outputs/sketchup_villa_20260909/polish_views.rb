# encoding: UTF-8
require 'sketchup.rb'
require 'json'
UI.start_timer(1,false) do
  out='E:/众墅之家AI赋能平台底座/outputs/sketchup_villa_20260909'
  begin
    m=Sketchup.active_model
    raise 'Wrong model' unless m.get_attribute('模型说明','来源')
    root=m.entities.grep(Sketchup::Group).find{|g|g.name.start_with?('众墅之家')}
    unless m.get_attribute('模型说明','檐底校正')
      roof=root.entities.grep(Sketchup::Group).find{|g|g.name.start_with?('06')}
      roof.entities.grep(Sketchup::Group).select{|g|['中央挑檐','宽挑檐'].include?(g.name)}.each do |cg|
        cg.entities.grep(Sketchup::ComponentInstance).each do |i|
          width=i.bounds.width.to_m
          if [0.13,0.17,0.18].any?{|w|(width-w).abs<0.001}
            i.transform!(Geom::Transformation.translation([0,-0.12.m,-0.15.m]))
          end
        end
      end
      m.set_attribute('模型说明','檐底校正',true)
    end
    ro=m.rendering_options
    {'EdgeDisplayMode'=>0,'DrawSilhouettes'=>false,'DrawProfilesOnly'=>false,'DrawBackEdges'=>false,'DrawGround'=>false,'DrawHorizon'=>false,'DisplaySketchAxes'=>false,'ModelTransparency'=>false,'MaterialTransparency'=>true,'Texture'=>true,'BackgroundColor'=>Sketchup::Color.new(225,232,239),'SkyColor'=>Sketchup::Color.new(183,210,231)}.each{|k,v|ro[k]=v}
    si=m.shadow_info
    si['DisplayShadows']=true;si['Light']=82;si['Dark']=30;si['UseSunForAllShading']=true
    candidates=[]
    48.times do |i|
      t=Time.utc(2026,9,9,0,0,0)+i*1800
      si['ShadowTime']=t
      v=si['SunDirection']
      score= v.z>0.35 ? (-0.65*v.y+0.40*v.z-0.28*v.x) : -10
      candidates << [score,t,v.to_a]
    end
    best=candidates.max_by(&:first);si['ShadowTime']=best[1]
    m.styles.update_selected_style
    view=m.active_view;pt=lambda{|x,y,z|[x.m,y.m,z.m]}
    m.pages.each{|pg|pg.use_rendering_options=false;pg.use_shadow_info=false}
    m.layers['09 庭院景观'].visible=false
    view.camera=Sketchup::Camera.new(pt.call(0,-35,5.60),pt.call(0,0,5.60),Z_AXIS,false);view.camera.height=13.2.m
    m.pages[0].update
    view.refresh;view.write_image(filename:File.join(out,'01_front.png'),width:2100,height:1700,antialias:true)
    m.layers['09 庭院景观'].visible=true
    view.camera=Sketchup::Camera.new(pt.call(23,-32,19),pt.call(0,0.8,3.9),Z_AXIS,true);view.camera.fov=43
    m.pages[1].update
    view.refresh;view.write_image(filename:File.join(out,'02_perspective.png'),width:2100,height:1550,antialias:true)
    m.layers['09 庭院景观'].visible=false
    view.camera=Sketchup::Camera.new(pt.call(0,-25,4.25),pt.call(0,0,4.25),Z_AXIS,false);view.camera.height=8.35.m
    m.pages[2].update
    view.refresh;view.write_image(filename:File.join(out,'03_entry_detail.png'),width:1500,height:1600,antialias:true)
    m.layers['09 庭院景观'].visible=true
    view.camera=Sketchup::Camera.new(pt.call(0,-48,3.25),pt.call(0,0,5.65),Z_AXIS,true);view.camera.fov=24.5
    m.pages[3].update
    view.refresh;view.write_image(filename:File.join(out,'04_front_perspective.png'),width:2100,height:1700,antialias:true)
    m.styles.update_selected_style
    raise 'Save failed' unless m.save(File.join(out,'众墅之家_效果图复原_14米假定尺寸.skp'))
    File.write(File.join(out,'final_check.json'),JSON.pretty_generate({file:m.path,status:'polished_and_saved',sun:best,style:ro.to_h,scenes:m.pages.map(&:name),entity_count:m.entities.count,definitions:m.definitions.count}))
  rescue Exception=>e
    File.write(File.join(out,'polish_error.txt'),e.full_message)
  end
end
