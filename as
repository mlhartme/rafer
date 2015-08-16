on run(arguments)
  tell application "Image Events"
    launch
    repeat with arg in arguments
      set dng to POSIX file (arg as text) as alias
      set jpg to (POSIX path of dng) & ".jpg"
      log "convert " & dng & " -> " & jpg
      set dngRef to open dng
      tell dngRef
        save in jpg as JPEG
      end tell
      close dngRef
    end repeat
  end tell
end run
