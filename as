on run(arguments)
tell application "Image Events"
    launch
    set theImage to POSIX file (first item of arguments) as alias
    set theImageReference to open theImage
    tell theImageReference
       save in "/Users/mhm/foo.jpg" as JPEG
    end tell
end tell
end run

#tell theImageReference
#    set theImageName to name
#    save in "foo.jpg" as JPEG
#end tell
