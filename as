#on run argv
#  return "hello, " & item 1 of argv & "."
#end run

tell application "Image Events"
    launch
    set theImage to POSIX file "/Users/mhm/foo.dng"
    set theImageReference to open theImage
    tell theImageReference
       save in "/Users/mhm/foo.jpg" as JPEG
    end tell
end tell

#tell theImageReference
#    set theImageName to name
#    save in "foo.jpg" as JPEG
#end tell
