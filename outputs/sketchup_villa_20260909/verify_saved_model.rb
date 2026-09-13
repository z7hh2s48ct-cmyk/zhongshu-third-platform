# encoding: UTF-8
require 'sketchup.rb'
require 'json'
ZS_SKIP_AUTOBUILD=true
load 'E:/众墅之家AI赋能平台底座/outputs/sketchup_villa_20260909/build_villa.rb'
UI.start_timer(1,false) do
  out='E:/众墅之家AI赋能平台底座/outputs/sketchup_villa_20260909'
  begin
    m=Sketchup.active_model
    raise 'Final detail metadata missing' unless m.get_attribute('模型说明','花格细化')
    root=m.entities.grep(Sketchup::Group).find{|g|g.name.start_with?('众墅之家')}
    groups=root.entities.grep(Sketchup::Group)
    raise 'Missing model groups' unless groups.length==10
    raise 'Missing scenes' unless m.pages.length==4
    raise 'Wrong display style' if m.rendering_options['ModelTransparency']
    site=groups.find{|g|g.name.start_with?('09')}
    hs=ZSReferenceVilla;hs.instance_variable_set(:@model,m)
    cubes={};m.definitions.each{|df|cubes[df.name.sub('基本块_','')]=df if df.name.start_with?('基本块_')};hs.instance_variable_set(:@cubes,cubes)
    unless m.get_attribute('模型说明','步道连续')
      [-8.12,-7.70].each{|yy|4.times{|i|hs.box(site.entities,-2.15+i*1.08,yy,-0.035,1.055,0.395,0.075,m.materials['步道浅灰石'],'步道接续石板')}}
      m.set_attribute('模型说明','步道连续',true)
    end
    v=m.active_view;pt=lambda{|x,y,z|[x.m,y.m,z.m]}
    m.layers['09 庭院景观'].visible=true
    v.camera=Sketchup::Camera.new(pt.call(23,-32,19),pt.call(0,0.8,3.9),Z_AXIS,true);v.camera.fov=43
    m.pages[1].update;v.refresh
    v.write_image(filename:File.join(out,'02_perspective.png'),width:2100,height:1550,antialias:true)
    v.camera=Sketchup::Camera.new(pt.call(0,-48,3.25),pt.call(0,0,5.65),Z_AXIS,true);v.camera.fov=24.5
    m.pages[3].update;v.refresh
    v.write_image(filename:File.join(out,'04_front_perspective.png'),width:2100,height:1700,antialias:true)
    raise 'Failed saving verified file' unless m.save(File.join(out,'众墅之家_14米外观模型_最终.skp'))
    report=JSON.parse(File.read(File.join(out,'model_report.json')))
    report['reopened_in_sketchup_2022']=true;report['walkway_continuous']=true;report['file']=m.path
    report['verified_at']=Time.now.to_s;report['file_bytes']=File.size(m.path)
    File.write(File.join(out,'model_report.json'),JSON.pretty_generate(report))
    File.write(File.join(out,'final_check.json'),JSON.pretty_generate(report))
  rescue Exception=>e
    File.write(File.join(out,'verify_error.txt'),e.full_message)
  end
end
