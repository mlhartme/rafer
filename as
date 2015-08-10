on run(arguments)
  tell application "Image Events"
    launch
    repeat with x in arguments
      set img to x as text
      set img to POSIX file img as alias
      set out to "/Users/mhm/foo" & ".jpg"
      log "convert " & img & " -> " & out
      set r to open img
      tell r
        save in out as JPEG
      end tell
    end repeat
  end tell
end run
