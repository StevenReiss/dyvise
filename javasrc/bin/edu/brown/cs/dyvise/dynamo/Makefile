#
#		Make.pass
#
#	This file is the default makefile for source directorys.  It causes
#	the make to actually be done in the proper binary directory.  Each
#	source directory should link the name `Makefile` to this file.
#
#
# Copyright 1990 Steven P. Reiss, Brown University - All rights reserved.
#
#


MAKE=gnumake -w $(MFLAGS)
PARM=
SHELL= /bin/sh

SYSROOT= `dyvisebasepath`
OURNAME= Make.pass
BINDIR= `dyvisebinpath`
DATAPATH= `dyvisedatapath | relpath`
SRCDIR= `pwd`
PRONAME= `echo ${PWD:t}`

.SUFFIXES:

all .DEFAULT:
	(cd $(BINDIR); $(MAKE) $(PARM) $@)

create:
	$(BROWN_DYVISE_DYVISE)/bin/dyvisesetup
	rm -f Makefile
	ln -s $(DATAPATH)/$(OURNAME) Makefile
	rm -f $(BINDIR)/Makefile
	(cd $(BINDIR); ln -s $(DATAPATH)/Make.template Makefile )
	(cd $(BINDIR); $(MAKE) fixcreate)

rcssetup:
	- if [ ! -d RCS ]; then mkdir RCS; else true; fi
	(cd $(BINDIR); $(MAKE) $(PARM) $@)

.PHONY: all create rcssetup


createc++:
	$(BROWN_DYVISE_DYVISE)/bin/dyvisecreatec++ $(SRCDIR)


createjava:
	$(BROWN_DYVISE_DYVISE)/bin/dyvisecreatejava $(SRCDIR)







