
#node images
for i in 1 2 3 4 5 6 7 8 
do
  echo Compiling for node $i
  cat Makefile | sed "s/_ADDRESS=.*/_ADDRESS=$i/" > Makefile.2; mv Makefile.2 Makefile; make switchmodule; mv build/switchmodule/main.exe build/switchmodule/node$i.exe
done  

# voice module
cat Makefile | sed "s/_ADDRESS=.*/_ADDRESS=11/" > Makefile.2; mv Makefile.2 Makefile; make voicemodule; mv build/voicemodule/main.exe build/voicemodule/voicemodule11.exe

