/********************************************************************************/
/*										*/
/*		dymtimain.C							*/
/*										*/
/*	Main entry and controller for JVMTI monitoring for DYPER		*/
/*										*/
/********************************************************************************/
/*	Copyright 2005 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2005, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 *  Permission to use, copy, modify, and distribute this software and its	 *
 *  documentation for any purpose other than its incorporation into a		 *
 *  commercial product is hereby granted without fee, provided that the 	 *
 *  above copyright notice appear in all copies and that both that		 *
 *  copyright notice and this permission notice appear in supporting		 *
 *  documentation, and that the name of Brown University not be used in 	 *
 *  advertising or publicity pertaining to distribution of the software 	 *
 *  without specific, written prior permission. 				 *
 *										 *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		 *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
 *  OF THIS SOFTWARE.								 *
 *										 *
 ********************************************************************************/

/* RCS: $Header: /pro/spr_cvs/pro/dyvise/dymti/src/dymtimain.C,v 1.4 2009-04-11 23:45:16 spr Exp $ */


/*********************************************************************************
 *
 * $Log: dymtimain.C,v $
 * Revision 1.4  2009-04-11 23:45:16  spr
 * Clean up monitoring.
 *
 * Revision 1.3  2009-03-20 02:05:20  spr
 * Add memory reference dumping.
 *
 * Revision 1.2  2008-11-12 14:20:12  spr
 * kvm no longer exists in mac os/x
 *
 * Revision 1.1.1.1  2008-10-22 13:16:47  spr
 * Original version from WADI
 *
 *
 ********************************************************************************/



#include "dymti_local.H"

#include <dlfcn.h>

#ifdef APPLE_OLD
#include <kvm.h>
#include <sys/sysctl.h>
#define USE_KVM 1
#endif


/********************************************************************************/
/*										*/
/*	Local function definitions						*/
/*										*/
/********************************************************************************/

extern "C" {

   jint Agent_OnLoad(JavaVM *,char *,void *);
   jint Agent_OnAttach(JavaVM *,char *,void *);
   void Agent_OnUnload(JavaVM *);

};




/********************************************************************************/
/*										*/
/*	Local Storage								*/
/*										*/
/********************************************************************************/

static DymtiMain	dymti_main = NULL;



/********************************************************************************/
/*										*/
/*	Initial entry from JVMTI						*/
/*										*/
/********************************************************************************/

jint
Agent_OnLoad(JavaVM * jvm,char * opts,void *)
{
   jvmtiEnv * jvmti;

   jvm->GetEnv((void **) &jvmti,JVMTI_VERSION_1_0);

   dymti_main = new DymtiMainInfo(jvm,jvmti,opts);
   dymti_main->initialSetup();

   return JNI_OK;
}




jint
Agent_OnAttach(JavaVM * jvm,char * opts,void *)
{
   jvmtiEnv * jvmti;

   jvm->GetEnv((void **) &jvmti,JVMTI_VERSION_1_0);

   dymti_main = new DymtiMainInfo(jvm,jvmti,opts);
   dymti_main->initialSetup();

   return JNI_OK;
}




void
Agent_OnUnload(JavaVM * jvm)
{
   dymti_main->finalize();
}




/********************************************************************************/
/*										*/
/*	Constructors/Destructors						*/
/*										*/
/********************************************************************************/

DymtiMainInfo::DymtiMainInfo(JavaVM * jvm,DymtiEnv env,const char * opts)
{
   java_vm = jvm;
   jvmti_env = env;

   char hbuf[1024];
   gethostname(hbuf,1024);
   char nbuf[2048];
   sprintf(nbuf,"%d@%s",getpid(),hbuf);

   user_options["MID"] = DYPER_MESSAGE_BUS;
   user_options["PID"] = nbuf;

   if (opts != NULL) {
      for (StringTokenizer tok(opts,","); tok.hasMoreTokens(); ) {
	 StdString t = tok.nextToken();
	 StdString v = "true";
	 int idx = t.find_first_of('=');
	 if (idx >= 0) {
	    v = t.substr(idx+1);
	    t = t.substr(0,idx);
	  }
	 user_options[t] = v;
       }
    }

   using_monitor = new DymtiMonitorInfo(this,java_vm,jvmti_env);
   using_connection = new DymtiConnectionInfo(this);
   using_native = new DymtiNativeInfo(this);

   cerr << "DYMTI: Loaded" << endl;
}



DymtiMainInfo::~DymtiMainInfo()
{
   delete using_monitor;
   delete using_connection;
}




/********************************************************************************/
/*										*/
/*	Startup code								*/
/*										*/
/********************************************************************************/

void
DymtiMainInfo::initialSetup()
{
   bool attach = !(user_options["noattach"] == "true") ;

   using_monitor->initialSetup(attach);
}



/********************************************************************************/
/*										*/
/*	Shutdown code								*/
/*										*/
/********************************************************************************/

void
DymtiMainInfo::finalize()
{ }




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

CStdString
DymtiMainInfo::getOption(CStdString c)		       { return user_options[c]; }



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

void
DymtiMainInfo::allocMonitor(IvyXmlNode msg)
{
   using_monitor->allocMonitor(msg);
}



void
DymtiMainInfo::stopThreads()
{
   using_monitor->stopThreads();
}


void
DymtiMainInfo::resumeThreads()
{
   using_monitor->resumeThreads();
}



void
DymtiMainInfo::buildMemoryModel(IvyXmlWriter& ost)
{
   using_monitor->dumpMemoryModel(ost);
}




/********************************************************************************/
/*										*/
/*	Attach methods								*/
/*										*/
/********************************************************************************/

static	StdString	processArgs(char **);
static	void		processArg(StdString&,CStdString,bool&);



StdString
DymtiMainInfo::findProcess()
{
   char ** props;
   jint nprop;
   char * pv;

   StdString dname;

#ifdef USE_KVM
   kvm_t * vm = kvm_open(NULL,NULL,NULL,O_RDONLY,"DYMTI: ");
   if (vm != NULL) {
      int cnt = 0;
      struct kinfo_proc * kin = kvm_getprocs(vm,KERN_PROC_PID,getpid(),&cnt);
      char ** argv = kvm_getargv(vm,kin,1024);
      dname = processArgs(argv);
    }
#endif

   char *** nxargvp = (char ***) dlsym(RTLD_DEFAULT,"_NXArgv");
   if (nxargvp == NULL) nxargvp = (char ***) dlsym(RTLD_DEFAULT,"NXArgv");
   if (dname == "" && nxargvp != NULL) {
      char ** nxargv = *nxargvp;
      dname = processArgs(nxargv);
    }

   if (dname == "") {
      StringBuffer s;
      s << "/proc/" << getpid() << "/cmdline";
      IvyInputFile ifs = new IvyInputFileInfo(s.getCString());
      if (ifs->isValid()) {
	 bool skip = false;
	 for (int i = 0; ; ++i) {
	    StdString cmd = ifs->getLine(0);
	    if (cmd.empty()) break;
	    processArg(dname,cmd,skip);
	  }
	 ifs->close();
       }
    }

   char hbuf[1024];
   gethostname(hbuf,1024);

   StringBuffer buf;
   buf << "<PROCESS>\n";
   buf << "<PID>" << getpid() << "@" << hbuf << "</PID>";
   buf << "<COMMAND>" << dname << "</COMMAND>";
   buf << "</PROCESS>\n";

   return buf.getString();
}



static StdString
processArgs(char ** argv)
{
   StdString dname;
   bool skip = false;

   for (int i = 0; argv[i] != NULL; ++i) {
      StdString cmd = argv[i];
      processArg(dname,cmd,skip);
    }

   return dname;
}



static void
processArg(StdString& cmdline,CStdString cmd,bool& skip)
{
   if (cmdline == "" && !skip) {
      cmdline = cmd;
    }
   else if (cmdline != "") {
      cmdline += " " + cmd;
    }
   else if (cmd == "-cp" || cmd == "-classpath") skip = true;
   else skip = false;
}




/********************************************************************************/
/*										*/
/*	Parameter methods							*/
/*										*/
/********************************************************************************/

void
DymtiMainInfo::setParameter(CStdString nm,CStdString val)
{
   using_monitor->setParameter(nm,val);
}



/* end of dymtimain.C */
