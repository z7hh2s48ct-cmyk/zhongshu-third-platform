require 'sketchup.rb'
UI.start_timer(1.0, false) do
  begin
    File.write(File.join(__dir__, 'api_probe.txt'), "SketchUp #{Sketchup.version}\n#{Sketchup.active_model.title}\n#{Sketchup.active_model.entities.length}")
  rescue => e
    File.write(File.join(__dir__, 'api_error.txt'), e.full_message)
  end
end
