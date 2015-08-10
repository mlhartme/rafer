on run(arguments)
  tell application "Image Events"
    launch
    repeat with x in arguments
      set dng to POSIX file (x as text) as alias
      set jpg to "/Users/mhm/foo.jpg"
      set posixPath to POSIX path of dng
      log "path " & posixPath
      log "convert " & dng & " -> " & jpg
      set dngRef to open dng
      tell dngRef
        save in jpg as JPEG
      end tell
    end repeat
  end tell
end run
