## image-metadata-processor

### how to run
* java 11
* run main method


### todo
* run report on which all file extensions are in the folder
* get dropbox to sync folder locally
    * i need an upgraded account to get that many files
* find create date in the exif metadata
    * couldn't find on first pass
    * this is the field that google photos is looking at
* what format does google photos want the date in?
* create logic to loop through jpg files
* there may be some heic files in there, figure out how many

### notes
useful exif tag docs https://www.awaresystems.be/imaging/tiff/tifftags/search.html?q=date&Submit=Find+Tags

google photos is using these 2 fields i'm pretty sure: (this is wrong!!)
* GPS Date Stamp
* GPS Time Stamp

google photos looks at date time original!!!

if gps datestamp isn't there, google may be looking at create date.

Official Exif tags are:
* GPSDateStamp
* GPSTimeStamp

Also set these:
* DateTimeOriginal
* CreateDate
* ModifyDate

I tested this by running the jhead command to change all timestamps then uploaded to google photos. Google photos still showed the old date time from the gps fields.


gps datestamp debug value:
[TagInfo. tag: 29 (0x1d, name: GPSDateStamp]
field type ascii
length: 11
directory type: EXIF_DIRECTORY_GPS


### exif tool
SYNOPSIS
  Reading
    exiftool [*OPTIONS*] [-*TAG*...] [--*TAG*...] *FILE*...

  Writing
    exiftool [*OPTIONS*] -*TAG*[+-<]=[*VALUE*]... *FILE*...

  Copying
    exiftool [*OPTIONS*] -tagsFromFile *SRCFILE* [-*SRCTAG*[>*DSTTAG*]...]
    *FILE*...

https://exiftool.org/TagNames/GPS.html

gps timestamp format:
https://stackoverflow.com/questions/4879435/android-put-gpstimestamp-into-jpg-exif-tags
Proper format for GPSTimeStamp attribute for sample time 14:22:32 is

"14/1,22/1,32/1"

gps datestamp format:
0x001d	GPSDateStamp	string[11]	(when writing, time is stripped off if present, after adjusting date/time to UTC if time includes a timezone. Format is YYYY:mm:dd)

./exiftool.exe -GPSDateStamp=1993:03:27 'C:\Users\josep\Desktop\jpeg-testing\Photo Jun 09, 6 38 10 AM.jpg'


./exiftool.exe -DateTimeOriginal=1992:03:27-01:02:03 -CreateDate=1992:03:27-01:02:03 -ModifyDate=1992:03:27-01:02:03 C:\Users\josep\Desktop\jpeg-testing\IMG_4676.JPG

[datetime]::ParseExact("20181010134412",'yyyyMMddHHmmss',$null)
[datetime]::ParseExact("Jun 13, 1994, 5:36:10",'MMM dd, yyyy, HH:mm:ss',$null)


[datetimeoffset]::FromUnixTimeMilliseconds(771528970000).DateTime


 .\exiftool.exe  'C:\Users\josep\Desktop\jpeg-testing\Photo Jun 09, 6 38 10 AM.jpg'

this WORKS!!!!
./exiftool.exe -DateTimeOriginal=1991:03:27-01:02:03 'C:\Users\josep\Desktop\jpeg-testing\Photo Jun 09, 6 38 10 AM.jpg'
