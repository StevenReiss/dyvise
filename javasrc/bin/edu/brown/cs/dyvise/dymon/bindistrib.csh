#! /bin/csh -fx

rm -rf DYVISEBIN
mkdir DYVISEBIN
mkdir DYVISEBIN/lib

set H = `pwd`

cd /pro/ivy/java
find . -follow -name '*.class' -print | \
	fgrep -v tea/iced >! ivy.files
jar cf $H/DYVISEBIN/ivy.jar `cat ivy.files`
rm ivy.files
echo ivy.jar created

cd /pro/dyvise/java
find . -follow -name '*.class' -print >! dyvise.files
jar cf $H/DYVISEBIN/dyvise.jar `cat dyvise.files`
rm dyvise.files
echo dyvise.jar created

cd $H/DYVISEBIN
mkdir tools
cd tools
jar tf /pro/ivy/lib/tools.jar | fgrep attach  | fgrep .class >! tools.files
jar xf /pro/ivy/lib/tools.jar
jar cf $H/DYVISEBIN/tools.jar `cat tools.files`
echo tools.jar created

cd $H/DYVISEBIN
cp dyvise.jar dyvisefull.jar
mergejar dyvisefull.jar ivy.jar
mergejar dyvisefull.jar /pro/ivy/lib/jikesbt.jar
mergejar dyvisefull.jar tools.jar
mergejar dyvisefull.jar /pro/dyvise/lib/quadprog.jar
jar ufe dyvisefull.jar edu.brown.cs.dyvise.dymon.DymonRun
echo dyvisefull.jar created

cp ivy.jar ivyfull.jar
mergejar ivyfull.jar /pro/ivy/lib/jikesbt.jar
echo ivyfull.jar created

cd $H/DYVISEBIN
mv ivy.jar dyvise.jar lib
rm -rf tools tools.jar

cd $H

mkdir DYVISEBIN/lib/i686
mkdir DYVISEBIN/lib/mac64
mkdir DYVISEBIN/lib/x86_64
mkdir DYVISEBIN/lib/i386

cp README.BIN DYVISEBIN

cp /pro/dyvise/lib/*.xml /pro/dyvise/lib/dyper.jar DYVISEBIN/lib
mkdir DYVISEBIN/lib/images
cp -R /pro/dyvise/lib/images/*.png DYVISEBIN/lib/images

foreach a (i686 x86_64 i386 mac64)
   cp /pro/dyvise/lib/$a/libdymti.* DYVISEBIN/lib/$a
   cp /pro/ivy/lib/$a/libnative.* DYVISEBIN/lib/$a
   cp /pro/ivy/lib/$a/libmince.* DYVISEBIN/lib/$a
end

cd DYVISEBIN/lib/mac64
cp /pro/dyvise/lib/mac64/libxerces-c-3.0.dylib .
ln -s libxerces-c-3.0.dylib libxerces-c.dylib
cd $H

mkdir DYVISEBIN/tmp
mkdir DYVISEBIN/tmp/dyper
mkdir DYVISEBIN/trace

cd DYVISEBIN
tar czf $H/dyvisebin.tgz .

cd $H
ls -l dyvisebin.tgz
echo done
