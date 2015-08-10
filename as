on run(arguments)
  tell application "Image Events"
    launch
    repeat with x in arguments
      set img to POSIX file (x as text) as alias
      set jpg to "/Users/mhm/foo" & ".jpg"
      log "convert " & img & " -> " & jpg
      set r to open img
      tell r
        save in jpg as JPEG
      end tell
    end repeat
  end tell
end run
