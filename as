on run(arguments)
  tell application "Image Events"
    launch
    repeat with x in arguments
      set theImage to x as text
      set theImage to POSIX file theImage as alias
      set out to ((x as text) & ".jpg")
      log "convert " & theImage & " -> " & out
      set theImageReference to open theImage
      tell theImageReference
        save in "/Users/mhm/foo.jpg" as JPEG
      end tell
    end repeat
  end tell
end run
