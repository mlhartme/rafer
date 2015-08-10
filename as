on run(arguments)
  tell application "Image Events"
    launch
    repeat with x in arguments
      set dng to POSIX file (x as text) as alias
      set jpg to "/Users/mhm/foo" & ".jpg"
      log "convert " & dng & " -> " & jpg
      set r to open dng
      tell r
        save in jpg as JPEG
      end tell
    end repeat
  end tell
end run
