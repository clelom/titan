This directory contains the code for the Titan framework. The framework can be compiled either for a J2ME CDC Profile (mobile phone) platform, or for a desktop version.
Files that can be used for both platforms are in the "common" subdirectory. Files that are desktop-only are in the "desktop" subdirectory, while "mobile" contains J2ME files.

How to create a DESKTOP project
===============================
Create a source directory "src" and copy the following java packages into it:

common/titancommon     -> src/titancommon
desktop/titan          -> src/titan
desktop/TOSCommandLine -> src/TOSCommandLine

Libraries to be included are found in the "libs" subdirectory.

Execute the Titan framework using TOSCommandLine.TOSCommandLine as the main class.

How to create a WINDOWS MOBILE project
======================================
For NetBeans follow the Guide here: http://netbeans.org/community/releases/55/1/mobilitycdc-install.html
(Download NetBeans with MobilityPack and install CrEme 4.10 SDK: http://nsicom.com/shared/CrEmeDevSup410.exe )
Create a project for the CDC platform and checkout the following subdirectories in the "src" folder NetBeans creates for you:

common/titancommon -> src/titancommon
mobile/titan       -> src/titan
mobile/net         -> src/net

Include the "libs/swing-layout-1.0.3.jar" and the "libs/bluecove-2.0.2.jar" for compilation.
The main class is titan.mobile.TitanMobile
When using IBM's J9 virtual machine on the mobile phone, start Titan with a *.lnk file containing the following string:

255#"\Storage card\J9\PPRO11\bin\j9.exe" "-jcl:ppro11" "-Xbootclasspath/p:\Storage card\J9\Titan\lib\bluecove-2.0.2.jar;\Storage card\J9\Titan" "-Duser.dir=\Storage card\J9\Titan\" "-jar" "\Storage card\J9\Titan\TitanMobileWM.jar"

Note 1: When using Windows Mobile 6, the J9 virtual machine may crash. Use the additinoal flag "-nojit" to disable Just-In-Time compilation to fix it.
Note 2: When customizing the LNK file, the total file size cannot be larger than 256 bytes!
