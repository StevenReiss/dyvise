#
# DYVISE Library makefile
#
# Copyright 1994 Steven P. Reiss, Brown University - All rights reserved.
#
#

SYSTEM= dyvise

ALL=

PFILES= Makefile tracefiles
JARFILES= gnu.jar xml.jar jdeps.jar

ACTIVE= $(PFILES) $(JARFILES)
DATAACTIVE= data/*.mural data/*.dom data/*.vis data/*.bee data/*.xml
TEXTUREACTIVE = textures/*.tif
FLAVORACTIVE = flavors/*.hive

SHELL= /bin/sh


all .DEFAULT:
	@echo $@ Nothing to do.

create:
	-if [ ! -d $(BROWN_DYVISE_ARCH) ] ; then mkdir $(BROWN_DYVISE_ARCH); else true; fi

print:
	pr $(PFILES) | lpr -low

pribm:
	/bin/pr -l62 -f $(PFILES) | lpr -Pibm

prim:
	imprint -Pim0 -2 $(PFILES)

distrib_dir:
	rm -rf $(DISTRIB_DIR)/lib
	mkdir $(DISTRIB_DIR)/lib
	mkdir $(DISTRIB_DIR)/lib/textures
	mkdir $(DISTRIB_DIR)/lib/flavors
	mkdir $(DISTRIB_DIR)/lib/data
	cp $(ACTIVE) $(DISTRIB_DIR)/lib
	cp $(DATAACTIVE) $(DISTRIB_DIR)/lib/data
	cp $(TEXTUREACTIVE) $(DISTRIB_DIR)/lib/textures
	cp $(FLAVORACTIVE) $(DISTRIB_DIR)/lib/flavors

ourClib:
	-rm -rf clibdir
	mkdir clibdir
	(cd clibdir; ar x /opt/SUNWspro6.0/WS6/lib/libCstd.a)
	(cd clibdir; CC -G -o ../sol/libourCstd.so *.o)
	rm -rf clibdir



REALPROLIB=/maytag/pro0

bindistrib:
	mkdir $(DISTRIB_BIN)/lib
	mkdir $(DISTRIB_BIN)/lib/textures
	mkdir $(DISTRIB_BIN)/lib/flavors
	mkdir $(DISTRIB_BIN)/lib/data
	cp $(ACTIVE) $(DISTRIB_BIN)/lib
	cp $(DATAACTIVE) $(DISTRIB_BIN)/lib/data
	cp $(TEXTUREACTIVE) $(DISTRIB_BIN)/lib/textures
	cp $(FLAVORACTIVE) $(DISTRIB_BIN)/lib/flavors
	cp bloom.jar $(DISTRIB_BIN)/lib
	mkdir $(DISTRIB_BIN)/lib/sol
	-cp sol/* $(DISTRIB_BIN)/lib/sol
	mkdir $(DISTRIB_BIN)/lib/i686
	-cp i686/* $(DISTRIB_BIN)/lib/i686
	cp $(REALPROLIB)/lib.solaris/libxerces* $(DISTRIB_BIN)/lib/sol
	cp $(REALPROLIB)/lib.solaris/libourCstd.so $(DISTRIB_BIN)/lib/sol
	-cp /cs/src/Qt/qt-2.2.1/lib/libqt.so.2 $(DISTRIB_BIN)/lib/sol
	cp $(REALPROLIB)/lib.lintel/libxerces* $(DISTRIB_BIN)/lib/i686
	cp $(REALPROLIB)/lib.lintel/libdemangle.so $(DISTRIB_BIN)/lib/i686
	cp $(REALPROLIB)/lib.lintel/libXrender* $(DISTRIB_BIN)/lib/i686
