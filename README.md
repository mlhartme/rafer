# rafer

Set your Fuji to take raw+jpg, use rafer to fetch these images from the memory card,
geotag them, and store them one your local machine and backup locations.

Prerequisites: 
   * Mac OS
   * Java 8
   * exiftool: http://www.sno.phy.queensu.ca/~phil/exiftool/

Tracking tools Know how:
   * GeoTag Fotos
      * works find, can automatically upload into Dropbox
      * could be more battery efficient: drains battery no matted if the handy is moved
   * myTracks
      * you have to save a track before restart or running out of battery :(  
      * you have to restart tracking after every reboot :(


## Directories

* Ravs: where to store recent rafs - I use a local drive for this
* Jpegs: where to store recent jpegs - I use Google Drive or Dropbox for this
* Backups: where to permanently store ravs and jpegs. 
  Includes everything from rafs and jpegs, thus, rafs and jpegs are kind of an inbox