#
# One has to create hard links as javac cannot follow softlinks
#
#

TITAN_LIBS=C:\cygwin\opt\tinyos-2.x\support\sdk\java\tinyos.jar;C:\cygwin\opt\tinyos-2.x-contrib\wearlab\support\sdk\java\libs\RXTXcomm.jar;C:\cygwin\opt\tinyos-2.x-contrib\wearlab\support\sdk\java\libs\swing-layout-1.0.3.jar


.links:
	@echo Creating source links..
	@for javafile in `find common/titan/* | grep "\.java" | grep -v "\.svn" | sed "s/common\(.*\)/\1/"`; do ln -f `cygpath -wa .`/common$$javafile desktop$$javafile; done
	@touch .links

.desktop: .links
	@echo Compiling java desktop...
	@cd desktop; javac -cp ".;$(TITAN_LIBS)" */*.java
	@cd desktop; javac -cp ".;$(TITAN_LIBS)" titan/applications/*.java
	@echo "java -cp \".;$(TITAN_LIBS)\" TOSCommandLine.TOSCommandLine" > desktop/run_titan
	@chmod a+x desktop/run_titan
  @echo Desktop version for Titan successfully comiled. You can start it in the desktop directory with "./run_titan"

all: .desktop
