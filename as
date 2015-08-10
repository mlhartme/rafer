on run(arguments)
  tell application "Image Events"
    launch
    repeat with x in arguments
      set theImage to x as text
      set theImage to POSIX file theImage as alias
      set out to "/Users/mhm/foo" & ".jpg"
      log "convert " & theImage & " -> " & out
      set r to open theImage
      tell r
        save in out as JPEG
      end tell
    end repeat
  end tell
end run
